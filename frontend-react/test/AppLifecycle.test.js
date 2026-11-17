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
    '/setSystem': { symbolic: false },
    '/buildModel': { success: true },
    '/toDOTSVG': [],
    '/interpretNext': { state: {}, transitions: [], svgs: [] },
  };
  await page.route(backend + '/**', async route => {
    const url = new URL(route.request().url());
    requests.push(url.pathname);
    if (url.pathname === '/setSystem') expect(url.searchParams.get('script')).toBe(script);
    expect(responses).toHaveProperty(url.pathname);
    await route.fulfill({ json: responses[url.pathname] });
  });
  await page.getByRole('tab', { name: /Interpreter/ }).click();
  await page.getByRole('button', { name: 'Start' }).click();
  await expect.poll(() => requests).toContain('/interpretNext');
  await expect.poll(() => requests).toContain('/toDOTSVG');
  expect(requests.filter(route => route === '/setSystem')).toHaveLength(1);
  expect(requests.indexOf('/setSystem')).toBeLessThan(requests.indexOf('/buildModel'));
});

test('rebuilding waits for every check to settle, even after Stop is requested', async ({ page }) => {
  const checks = [
    { id: 0, spec: 'first', url: '/modelCheck/0' },
    { id: 1, spec: 'second', url: '/modelCheck/1' },
  ];
  const responses = {
    '/setSystem': { symbolic: false },
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

test('failed model selection ends the build and does not submit another build request', async ({ page }) => {
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
  expect(requests).toEqual(['/setSystem']);
});
