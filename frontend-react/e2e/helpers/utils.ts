import { expect, Page } from "@playwright/test";
import fs from "node:fs";
import { fromDateWithTime, toDateWithTime } from "../pages/daily-data";

export const TEST_ORG_IGNORE = "ignore";
export const TEST_ORG_UP_RECEIVER_FULL_ELR = "FULL_ELR";
export const TEST_ORG_CP_RECEIVER_CSV = "CSV";
export const TEST_ORG_ELIMS_RECEIVER_ELR = "ELR_ELIMS";
export const TEST_ORG_AK_RECEIVER = "elr";
export async function scrollToFooter(page: Page) {
    // Scrolling to the bottom of the page
    await page.locator("footer").scrollIntoViewIfNeeded();
}

export async function scrollToTop(page: Page) {
    // Scroll to the top of the page
    await page.evaluate(() => window.scrollTo(0, 0));
}

export async function waitForAPIResponse(page: Page, requestUrl: string) {
    const response = await page.waitForResponse((response) => response.url().includes(requestUrl));
    return response.status();
}

export function noData(page: Page) {
    return page.getByText(/No available data/);
}
export function tableRows(page: Page) {
    return page.locator(".usa-table tbody").locator("tr");
}

export async function fulfillGoogleAnalytics(page: Page) {
    // fulfill GA request so that we don't log it and alter the metrics
    await page.route("https://www.google-analytics.com/**", (route) => route.fulfill({ status: 204, body: "" }));
}

export async function selectTestOrg(page: Page) {
    await page.goto("/admin/settings", {
        waitUntil: "domcontentloaded",
    });

    await waitForAPIResponse(page, "/api/settings/organizations");

    await page.getByTestId("gridContainer").waitFor({ state: "visible" });
    await page.getByTestId("textInput").fill(TEST_ORG_IGNORE);
    await page.getByTestId("ignore_set").click();
}
/**
 * Save session storage to file. Session storage is not handled in
 * playwright's storagestate.
 */
export async function saveSessionStorage(userType: string, page: Page) {
    const sessionJson = await page.evaluate(() => JSON.stringify(sessionStorage));
    fs.writeFileSync(`e2e/.auth/${userType}-session.json`, sessionJson, "utf-8");
}

export async function restoreSessionStorage(userType: string, page: Page) {
    const session = JSON.parse(fs.readFileSync(`e2e/.auth/${userType}-session.json`, "utf-8"));
    await page.context().addInitScript((session) => {
        for (const [key, value] of Object.entries<any>(session)) window.sessionStorage.setItem(key, value);
    }, session);
}

export function tableDataCellValue(page: Page, row: number, column: number) {
    return tableRows(page).nth(row).locator("td").nth(column).innerText();
}

/**
 * This method loops through all the cells in a column to compare with expected value.
 */
export async function expectTableColumnValues(page: Page, columnNumber: number, expectedValue: string) {
    const rowCount = await tableRows(page).count();

    for (let i = 0; i < rowCount; i++) {
        const columnValue = await tableRows(page).nth(i).locator("td").nth(columnNumber).innerText();
        expect(columnValue).toContain(expectedValue);
    }
}

/**
 * This method loops through all the cells in a date/time column to compare with a date value.
 */
export async function tableColumnDateTimeInRange(
    page: Page,
    columnNumber: number,
    fromDate: string,
    toDate: string,
    startTime: string,
    endTime: string,
) {
    let datesInRange = true;
    const rowCount = await tableRows(page).count();

    for (let i = 0; i < rowCount; i++) {
        const startDateTime = fromDateWithTime(fromDate, startTime);
        const endDateTime = toDateWithTime(toDate, endTime);
        const columnValue = await tableRows(page).nth(i).locator("td").nth(columnNumber).innerText();

        const columnDate = new Date(columnValue);

        if (!(columnDate >= startDateTime && columnDate < endDateTime)) {
            datesInRange = false;
            break;
        }
    }
    return datesInRange;
}
