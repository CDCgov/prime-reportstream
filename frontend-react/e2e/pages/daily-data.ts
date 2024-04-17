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

export async function setDate(page: Page, locator: string, offsetDate: number) {
    const currentDate = new Date();
    const newDate = format(
        new Date(currentDate.setDate(currentDate.getDate() - offsetDate)),
        "MM/dd/yyyy",
    );
    await page.locator(locator).fill(newDate);
    await page.keyboard.press("Tab");
    await expect(page.locator(locator)).toHaveValue(newDate);
    return new Date(newDate).toLocaleString();
}

export async function setTime(page: Page, locator: string, time: string) {
    await page.locator(locator).selectOption({ label: time });
    // await expect(page.locator(locator)).toHaveValue(time);
}
