import { expect, Page } from "@playwright/test";
import { format } from "date-fns";

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
    await expect(page.locator(".usa-table th").nth(0)).toHaveText(/Facility/);
    await expect(page.locator(".usa-table th").nth(1)).toHaveText(/Location/);
    await expect(page.locator(".usa-table th").nth(2)).toHaveText(/CLIA/);
    await expect(page.locator(".usa-table th").nth(3)).toHaveText(
        /Total tests/,
    );
    await expect(page.locator(".usa-table th").nth(4)).toHaveText(
        /Total positive/,
    );
}

export async function setFromDate(page: Page, offsetDate: number) {
    const currentDate = new Date();
    const fromDate = format(
        new Date(currentDate.setDate(currentDate.getDate() - offsetDate)),
        "MM/dd/yyyy",
    );
    await page.locator("#start-date").fill(fromDate);
    await page.keyboard.press("Tab");
    await expect(page.locator("#start-date")).toHaveValue(fromDate);
    return fromDate;
}

export async function setToDate(page: Page, offsetDate: number) {
    const currentDate = new Date();
    const toDate = format(
        new Date(currentDate.setDate(currentDate.getDate() - offsetDate)),
        "MM/dd/yyyy",
    );
    await page.locator("#end-date").fill(toDate);
    await page.keyboard.press("Tab");
    await expect(page.locator("#end-date")).toHaveValue(toDate);
    return toDate;
}

export async function setStartTime(page: Page, value: string) {
    await page.getByTestId("combo-box").nth(0).selectOption({ value: value });
}

export async function setEndTime(page: Page, value: string) {
    await page.locator("[name=end-time]").selectOption({ value: value });
}
