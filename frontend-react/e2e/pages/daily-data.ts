import { expect, Page } from "@playwright/test";
import { format } from "date-fns";
import { getTableRowCount } from "../helpers/utils";

const URL_DAILY_DATA = "/daily-data";
export const defaultStartTime = "12:00am";
export const defaultEndTime = "11:59pm";

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

export function resetButton(page: Page) {
    return page
        .getByTestId("filter-form")
        .getByRole("button", { name: "Reset" });
}

export function noData(page: Page) {
    return page.getByText(/No available data/);
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
    await page.locator(locator).fill(time);
    await expect(page.locator(locator)).toHaveValue(time);
    await page.keyboard.press("Tab");
    await page.keyboard.press("Tab");
}

export function fromDateWithTime(fromDate: string, startTime: string) {
    const fromDateTime = new Date(fromDate);
    const time = startTime.substring(0, startTime.length - 2);

    if (time) {
        const [startHours, startMinutes] = time.split(":").map(Number);
        fromDateTime.setHours(startHours, startMinutes, 0, 0);
    } else {
        fromDateTime.setHours(12, 0, 0);
    }
    return fromDateTime;
}

export function toDateWithTime(toDate: string, toTime: string) {
    const toDateTime = new Date(toDate);
    const time = toTime.substring(0, toTime.length - 2);

    if (time) {
        const [endHours, endMinutes] = time.split(":").map(Number);
        toDateTime.setHours(endHours, endMinutes, 0, 0);
    } else {
        toDateTime.setHours(11, 59, 0);
    }
    return toDateTime;
}

export async function getFilterStatus(
    page: Page,
    filters: (string | undefined)[],
) {
    // Refactor so that all logic is contained in here vs passing in
    // i.e. defaultStartTime
    const rowCount = await getTableRowCount(page);
    let filterStatus = `Showing (${rowCount ?? 0}) ${rowCount === 1 ? "result" : "results"} for: `;

    for (let i = 0; i < filters.length; i++) {
        filterStatus += filters[i];
        if (i < filters.length - 1) {
            filterStatus += ", ";
        }
    }

    await expect(page.getByTestId("filter-status")).toHaveText(filterStatus);
}
