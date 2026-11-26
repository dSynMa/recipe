const { test, expect } = require('@playwright/test');

test('opens the example in the R-CHECK editor with recipe syntax highlighting', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Build model' })).toBeEnabled();
  const editor = page.locator('.ace_editor');
  await expect.poll(() => editor.evaluate(el => el.env.editor.session.getMode().$id))
    .toBe('ace/mode/recipe');
  const example = await page.request.get('/example.rcp');
  expect(example.ok()).toBe(true);
  await expect.poll(() => editor.evaluate(el => el.env.editor.getValue())).toBe(await example.text());
});

test('only one tab opens the editor and a waiting tab takes over on close', async ({ page, context }) => {
  const second = await context.newPage();
  let exampleRequests = 0;
  await context.route('**/example.rcp', route => {
    exampleRequests++;
    return route.fulfill({ body: 'example' });
  });
  await Promise.all([page.goto('/'), second.goto('/')]);

  const build = { name: 'Build model' };
  const active = await Promise.race([page, second].map(async tab => {
    await tab.getByRole('button', build).waitFor();
    return tab;
  }));
  const waiting = active === page ? second : page;
  await expect(waiting.getByRole('status')).toContainText('open in another tab');
  await expect(waiting.getByRole('button', build)).toHaveCount(0);
  await expect.poll(() => exampleRequests).toBe(1);
  await active.close();
  await expect(waiting.getByRole('button', build)).toBeEnabled();
  await expect.poll(() => exampleRequests).toBe(2);
});
