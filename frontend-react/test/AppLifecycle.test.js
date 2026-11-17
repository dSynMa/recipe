const { test, expect } = require('@playwright/test');

const backend = 'http://localhost:54044';
const script = 'count := count + 2 & ready = TRUE\n';

test.beforeEach(async ({ page }) => {
  await page.route('**/example.rcp', route => route.fulfill({ body: script }));
  await page.goto('/');
  await expect(page.locator('.ace_line').first()).toHaveText(script.trim());
});

test('building to start the interpreter never reselects the model after build', async ({ page }) => {
  const requests = [];
  const responses = {
    '/prepareBuild': { buildId: 'test-build' },
    '/buildModel': { success: true },
    '/toDOTSVG': [],
    '/interpretNext': { state: {}, transitions: [], svgs: [] },
  };
  await page.route(backend + '/**', async route => {
    const url = new URL(route.request().url());
    requests.push(url.pathname);
    if (url.pathname === '/buildModel') expect(url.searchParams.get('script')).toBe(script);
    expect(responses).toHaveProperty(url.pathname);
    await route.fulfill({ json: responses[url.pathname] });
  });
  await page.getByRole('tab', { name: /Interpreter/ }).click();
  await page.getByRole('button', { name: 'Start' }).click();
  await expect.poll(() => requests).toContain('/interpretNext');
  await expect.poll(() => requests).toContain('/toDOTSVG');
  expect(requests.filter(route => route === '/prepareBuild')).toHaveLength(1);
  expect(requests.indexOf('/prepareBuild')).toBeLessThan(requests.indexOf('/buildModel'));
});

test('rebuilding waits for every check to settle, even after Stop is requested', async ({ page }) => {
  const checks = [
    { id: 0, spec: 'first', url: '/modelCheck/0' },
    { id: 1, spec: 'second', url: '/modelCheck/1' },
  ];
  const responses = {
    '/prepareBuild': { buildId: 'test-build' },
    '/buildModel': { success: true },
    '/toDOTSVG': [],
    '/modelCheck': { results: checks },
  };
  const pending = new Map();
  await page.route(backend + '/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/modelCheckStop' || checks.some(check => check.url === path)) {
      pending.set(path, route);
    } else {
      expect(responses).toHaveProperty(path);
      await route.fulfill({ json: responses[path] });
    }
  });
  const build = page.getByRole('button', { name: 'Build model', includeHidden: true });
  await build.click();
  await expect(build).toBeEnabled();
  await page.getByRole('tab', { name: 'Model Checker' }).click();
  await page.getByRole('button', { name: 'IC3', exact: true }).click();
  await page.getByRole('button', { name: 'Start' }).click();
  await expect.poll(() => pending.size).toBe(2);
  await expect(build).toBeDisabled();
  await page.getByRole('button', { name: 'Stop' }).click();
  await expect.poll(() => pending.has('/modelCheckStop')).toBe(true);
  await pending.get('/modelCheck/0').fulfill({ json: { result: 'true' } });
  await expect(page.getByText('pass', { exact: true })).toBeVisible();
  await expect(build).toBeDisabled();
  await pending.get('/modelCheck/1').abort();
  await expect(page.getByRole('heading', { name: /second/ })).toHaveCount(0);
  await expect(build).toBeDisabled();
  await pending.get('/modelCheckStop').fulfill({ json: {} });
  await expect(build).toBeEnabled();
  await page.getByRole('tab', { name: 'Editor' }).click();
  await build.click();
  await expect(page.getByText(/Building replaces the active model/)).toBeVisible();
});

test('failed build preparation ends the build and does not submit another build request', async ({ page }) => {
  const requests = [];
  await page.route(backend + '/**', route => {
    requests.push(new URL(route.request().url()).pathname);
    return route.fulfill({ json: { error: 'Invalid replacement model' } });
  });
  const build = page.getByRole('button', { name: 'Build model' });
  page.once('dialog', async dialog => {
    expect(dialog.message()).toBe('Invalid replacement model');
    await dialog.dismiss();
  });
  await build.click();
  await expect(build).toBeEnabled();
  expect(requests).toEqual(['/prepareBuild']);
});

test('cancelling an interpreter build suppresses late success and permits a new build', async ({ page }) => {
  let firstBuild;
  let buildCount = 0;
  const requests = [];
  await page.route(backend + '/**', async route => {
    const { pathname, searchParams } = new URL(route.request().url());
    requests.push(pathname);
    if (pathname === '/prepareBuild') {
      return route.fulfill({ json: { buildId: String(++buildCount) } });
    }
    if (pathname === '/buildModel' && searchParams.get('buildId') === '1') {
      firstBuild = route;
      return;
    }
    if (pathname === '/cancelBuild') {
      expect(searchParams.get('buildId')).toBe('1');
      await route.fulfill({ json: {} });
      return firstBuild.fulfill({ json: { success: true } });
    }
    if (pathname === '/buildModel') return route.fulfill({ json: { success: true } });
    if (pathname === '/toDOTSVG') return route.fulfill({ json: [] });
    throw new Error('Unexpected request ' + pathname);
  });
  await page.getByRole('tab', { name: /Interpreter/ }).click();
  await page.getByRole('button', { name: 'Start' }).click();
  await expect.poll(() => Boolean(firstBuild)).toBe(true);
  await page.getByRole('tabpanel', { name: /Interpreter/ }).getByRole('button', { name: 'Cancel build' }).click();
  await expect(page.getByRole('button', { name: 'Start' })).toBeEnabled();
  expect(requests).toEqual(['/prepareBuild', '/buildModel', '/cancelBuild']);
  await page.getByRole('tab', { name: 'Editor' }).click();
  await page.getByRole('button', { name: 'Build model' }).click();
  await expect.poll(() => requests.includes('/toDOTSVG')).toBe(true);
});

test('cancelling in the editor before preparation finishes skips the build', async ({ page }) => {
  let prepare;
  const requests = [];
  await page.route(backend + '/**', route => {
    const { pathname } = new URL(route.request().url());
    requests.push(pathname);
    if (pathname === '/prepareBuild') {
      prepare = route;
      return;
    }
    return route.fulfill({ json: {} });
  });
  const editor = page.getByRole('tabpanel', { name: 'Editor' });
  await editor.getByRole('button', { name: 'Build model' }).click();
  await editor.getByRole('button', { name: 'Cancel build' }).click();
  await expect(editor.getByRole('button', { name: /Cancelling build/ })).toBeDisabled();
  await expect.poll(() => Boolean(prepare)).toBe(true);
  await prepare.fulfill({ json: { buildId: 'pending' } });
  await expect(editor.getByRole('button', { name: 'Build model' })).toBeEnabled();
  expect(requests).toEqual(['/prepareBuild', '/cancelBuild']);
});
