import { expect, Page } from "@playwright/test";

const URL_DAILY_DATA = "/daily-data";
export async function goto(page: Page) {
    await page.goto(URL_DAILY_DATA, {
        waitUntil: "domcontentloaded",
    });
}

export async function title(page: Page) {
    await expect(page).toHaveTitle(
        /ReportStream - CDC's free, interoperable data transfer platform/,
    );
}

export async function tableHeaders(page: Page) {
    await expect(page.locator(".usa-table th").nth(0)).toHaveText(/Report ID/);
    await expect(page.locator(".usa-table th").nth(1)).toHaveText(
        /Time received/,
    );
    await expect(page.locator(".usa-table th").nth(2)).toHaveText(
        /File available until/,
    );
    await expect(page.locator(".usa-table th").nth(3)).toHaveText(/Items/);
    await expect(page.locator(".usa-table th").nth(4)).toHaveText(/Filename/);
    await expect(page.locator(".usa-table th").nth(5)).toHaveText(/Receiver/);
}
