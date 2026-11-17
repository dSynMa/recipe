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
