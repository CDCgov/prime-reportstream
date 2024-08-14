import fs from "node:fs";
import { BasePage } from "../pages/BasePage";
import { expect } from "../test";

export const TEST_ORG_IGNORE = "ignore";
export const TEST_ORG_UP_RECEIVER_FULL_ELR = "FULL_ELR";
export const TEST_ORG_AK_RECEIVER = "elr";
export async function scrollToFooter(basePage: BasePage) {
    // Scrolling to the bottom of the page
    await basePage.page.locator("footer").scrollIntoViewIfNeeded();
}

export async function scrollToTop(basePage: BasePage) {
    // Scroll to the top of the page
    await basePage.page.evaluate(() => window.scrollTo(0, 0));
}

export async function waitForAPIResponse(basePage: BasePage, requestUrl: string) {
    const response = await basePage.page.waitForResponse((response) => response.url().includes(requestUrl));
    return response.status();
}

export function noData(basePage: BasePage) {
    return basePage.page.getByText(/No available data/);
}
export function tableRows(basePage: BasePage) {
    return basePage.page.locator(".usa-table tbody").locator("tr");
}

export async function fulfillGoogleAnalytics(basePage: BasePage) {
    // fulfill GA request so that we don't log it and alter the metrics
    await basePage.page.route("https://www.google-analytics.com/**", (route) =>
        route.fulfill({ status: 204, body: "" }),
    );
}

export async function selectTestOrg(basePage: BasePage) {
    await basePage.page.goto("/admin/settings", {
        waitUntil: "domcontentloaded",
    });

    await waitForAPIResponse(basePage.page, "/api/settings/organizations");

    await basePage.page.getByTestId("gridContainer").waitFor({ state: "visible" });
    await basePage.page.getByTestId("textInput").fill(TEST_ORG_IGNORE);
    await basePage.page.getByTestId("ignore_set").click();
}
/**
 * Save session storage to file. Session storage is not handled in
 * playwright's storagestate.
 */
export async function saveSessionStorage(userType: string, basePage: BasePage) {
    const sessionJson = await basePage.page.evaluate(() => JSON.stringify(sessionStorage));
    fs.writeFileSync(`e2e/.auth/${userType}-session.json`, sessionJson, "utf-8");
}

export async function restoreSessionStorage(userType: string, basePage: BasePage) {
    const session = JSON.parse(fs.readFileSync(`e2e/.auth/${userType}-session.json`, "utf-8"));
    await basePage.page.context().addInitScript((session) => {
        for (const [key, value] of Object.entries<any>(session)) window.sessionStorage.setItem(key, value);
    }, session);
}

export function tableDataCellValue(basePage: BasePage, row: number, column: number) {
    return tableRows(basePage.page).nth(row).locator("td").nth(column).innerText();
}

/**
 * This method loops through all the cells in a column to compare with expected value.
 */
export async function expectTableColumnValues(basePage: BasePage, columnNumber: number, expectedValue: string) {
    const rowCount = await tableRows(basePage.page).count();

    for (let i = 0; i < rowCount; i++) {
        const columnValue = await tableRows(basePage.page).nth(i).locator("td").nth(columnNumber).innerText();
        expect(columnValue).toContain(expectedValue);
    }
}

/**
 * This method loops through all the cells in a date/time column to compare with a date value.
 */
export async function tableColumnDateTimeInRange(
    basePage: BasePage,
    columnNumber: number,
    fromDate: string,
    toDate: string,
    startTime: string,
    endTime: string,
) {
    let datesInRange = true;
    const rowCount = await tableRows(basePage.page).count();

    for (let i = 0; i < rowCount; i++) {
        const startDateTime = fromDateWithTime(fromDate, startTime);
        const endDateTime = toDateWithTime(toDate, endTime);
        const columnValue = await tableRows(basePage.page).nth(i).locator("td").nth(columnNumber).innerText();

        const columnDate = new Date(columnValue);

        if (!(columnDate >= startDateTime && columnDate < endDateTime)) {
            datesInRange = false;
            break;
        }
    }
    return datesInRange;
}

export function fromDateWithTime(date: string, time: string) {
    const fromDateTime = new Date(date);

    if (time) {
        // eslint-disable-next-line prefer-const
        let [hours, minutes] = time
            .substring(0, time.length - 2)
            .split(":")
            .map(Number);
        hours = hours + (time.indexOf("pm") !== -1 ? 12 : 0);
        fromDateTime.setHours(hours, minutes, 0, 0);
    } else {
        fromDateTime.setHours(0, 0, 0);
    }
    return fromDateTime;
}

export function toDateWithTime(date: string, time: string) {
    const toDateTime = new Date(date);

    if (time) {
        // eslint-disable-next-line prefer-const
        let [hours, minutes] = time
            .substring(0, time.length - 2)
            .split(":")
            .map(Number);
        hours = hours + (time.indexOf("pm") !== -1 ? 12 : 0);
        toDateTime.setHours(hours, minutes, 0, 0);
    } else {
        toDateTime.setHours(23, 59, 0);
    }
    return toDateTime;
}

export async function testFooter(basePage: BasePage) {
    await expect(basePage.page.footer).toBeAttached();
    await expect(basePage.page.footer).not.toBeInViewport();
    await scrollToFooter(basePage.page);
    await expect(basePage.page.footer).toBeInViewport();
    await expect(basePage.page.getByTestId("govBanner")).not.toBeInViewport();
    await scrollToTop(basePage.page);
    await expect(basePage.page.getByTestId("govBanner")).toBeInViewport();
}
