import { test, expect } from '@playwright/test';

test('has correect title', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle(/CDC Prime ReportStream/);
});

test('get started link', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('link', { name: 'Product' }).click();

  await expect(page).toHaveURL(/.*product\/overview/);
});
