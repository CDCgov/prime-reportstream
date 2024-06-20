import { expect, Page } from "@playwright/test";
import { format } from "date-fns";
import {
    MOCK_GET_RECEIVERS_AK,
    MOCK_GET_RECEIVERS_IGNORE,
} from "../mocks/organizations";

const URL_DAILY_DATA = "/daily-data";
const API_ORGANIZATIONS = "**/api/settings/organizations";

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

export function applyButton(page: Page) {
    return page.getByRole("button", {
        name: "Apply",
    });
}

export function searchInput(page: Page) {
    return page.locator("#search-field");
}

export function searchButton(page: Page) {
    return page.getByTestId("form").getByRole("button", { name: "Search" });
}

export function searchReset(page: Page) {
    return page.getByRole("button", { name: "Reset" }).nth(0);
}

export function filterReset(page: Page) {
    return page
        .getByTestId("filter-form")
        .getByRole("button", { name: "Reset" });
}

export function receiverDropdown(page: Page) {
    return page.locator("#receiver-dropdown");
}

export function startDate(page: Page) {
    return page.locator("#start-date");
}

export function endDate(page: Page) {
    return page.locator("#end-date");
}

export function startTime(page: Page) {
    return page.locator("#start-time");
}

export function endTime(page: Page) {
    return page.locator("#end-time");
}

export function startTimeClear(page: Page) {
    return page.getByTestId("combo-box-clear-button").nth(0);
}

export function endTimeClear(page: Page) {
    return page.getByTestId("combo-box-clear-button").nth(1);
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
export async function detailsTableHeaders(page: Page) {
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
        currentDate.setDate(currentDate.getDate() - offsetDate),
        "MM/dd/yyyy",
    );
    await page.locator(locator).fill(newDate);
    await page.keyboard.press("Tab");
    await expect(page.locator(locator)).toHaveValue(newDate);
    return newDate;
}

export async function setTime(page: Page, locator: string, time: string) {
    const startTime = page.locator(locator);
    await startTime.fill(time);
    await page.keyboard.press("Tab");
    await page.keyboard.press("Tab");
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

export function filterStatus(page: Page, filters: (string | undefined)[]) {
    // RowCount is not attainable with live data since it is returned from the API
    let filterStatus = ` for: `;

    for (let i = 0; i < filters.length; i++) {
        filterStatus += filters[i];
        if (i < filters.length - 1) {
            filterStatus += ", ";
        }
    }
    return filterStatus;
}

export async function mockGetOrgAlaskaReceiversResponse(
    page: Page,
    responseStatus = 200,
) {
    await page.route(`${API_ORGANIZATIONS}/ak-phd/receivers`, async (route) => {
        const json = MOCK_GET_RECEIVERS_AK;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetOrgIgnoreReceiversResponse(
    page: Page,
    responseStatus = 200,
) {
    await page.route(`${API_ORGANIZATIONS}/ignore/receivers`, async (route) => {
        const json = MOCK_GET_RECEIVERS_IGNORE;
        await route.fulfill({ json, status: responseStatus });
    });
}
