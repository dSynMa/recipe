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
