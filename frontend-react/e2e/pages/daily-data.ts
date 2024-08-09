import { expect, Page } from "@playwright/test";
import { format } from "date-fns";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "./BasePage";
import { API_WATERS_ORG } from "./report-details";
import { RSReceiver } from "../../src/config/endpoints/settings";
import { TEST_ORG_UP_RECEIVER_FULL_ELR } from "../helpers/utils";
import {
    MOCK_GET_DELIVERIES_IGNORE,
    MOCK_GET_DELIVERIES_IGNORE_FILENAME,
    MOCK_GET_DELIVERIES_IGNORE_FULL_ELR,
    MOCK_GET_DELIVERIES_IGNORE_REPORT_ID,
} from "../mocks/deliveries";
import { MOCK_GET_RECEIVERS_AK, MOCK_GET_RECEIVERS_IGNORE } from "../mocks/organizations";

export class DailyDataPage extends BasePage {
    static readonly URL_DAILY_DATA = "/daily-data";
    static readonly API_ORGANIZATIONS = "**/api/settings/organizations";
    protected _rsReceiver: RSReceiver[];

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: DailyDataPage.URL_DAILY_DATA,
                title: "ReportStream - CDC's free, interoperable data transfer platform",
                heading: testArgs.page.getByRole("heading", {
                    name: "Daily Data",
                }),
            },
            testArgs,
        );

        this._rsReceiver = [];
        this.addResponseHandlers([
            [DailyDataPage.API_ORGANIZATIONS, async (res) => (this._rsReceiver = await res.json())],
        ]);
        this.addMockRouteHandlers([
            this.createMockOrgIgnoreReceiversHandler(),
            this.createMockGetDeliveriesForOrgIgnoreHandler(),
            this.createMockGetDeliveriesForOrgIgnoreHandler(true),
            this.createMockGetDeliveriesForOrgIgnoreHandler(false, true),
            this.createMockGetDeliveriesForOrgIgnoreHandler(false, false, TEST_ORG_UP_RECEIVER_FULL_ELR),
        ]);
    }

    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.testArgs.storageState === this.testArgs.adminLogin.path;
    }

    createMockOrgIgnoreReceiversHandler(): RouteHandlerFulfillEntry {
        return [
            `${DailyDataPage.API_ORGANIZATIONS}/ignore/receivers`,
            () => {
                return {
                    json: MOCK_GET_RECEIVERS_IGNORE,
                };
            },
        ];
    }

    createMockGetDeliveriesForOrgIgnoreHandler(
        byReportId?: boolean,
        byFileName?: boolean,
        receiver?: string,
        responseStatus = 200,
    ): RouteHandlerFulfillEntry {
        if (receiver) {
            return [
                `${API_WATERS_ORG}/ignore.${receiver}/deliveries?*`,
                () => {
                    return {
                        json: MOCK_GET_DELIVERIES_IGNORE_FULL_ELR,
                        status: responseStatus,
                    };
                },
            ];
        } else if (byReportId) {
            return [
                `${API_WATERS_ORG}/ignore/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&reportId=729158ce-4125-46fa-bea0-3c0f910f472c`,
                () => {
                    return {
                        json: MOCK_GET_DELIVERIES_IGNORE_REPORT_ID,
                        status: responseStatus,
                    };
                },
            ];
        } else if (byFileName) {
            return [
                `${API_WATERS_ORG}/ignore/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&fileName=21c217a4-d098-494c-9364-f4dcf16b1d63-20240426204235.fhir`,
                () => {
                    return {
                        json: MOCK_GET_DELIVERIES_IGNORE_FILENAME,
                        status: responseStatus,
                    };
                },
            ];
        } else {
            return [
                `${API_WATERS_ORG}/ignore/deliveries?*`,
                () => {
                    return {
                        json: MOCK_GET_DELIVERIES_IGNORE,
                        status: responseStatus,
                    };
                },
            ];
        }
    }
}

const URL_DAILY_DATA = "/daily-data";
const API_ORGANIZATIONS = "**/api/settings/organizations";

export async function mockGetOrgAlaskaReceiversResponse(page: Page, responseStatus = 200) {
    await page.route(`${API_ORGANIZATIONS}/ak-phd/receivers`, async (route) => {
        const json = MOCK_GET_RECEIVERS_AK;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetOrgIgnoreReceiversResponse(page: Page, responseStatus = 200) {
    await page.route(`${API_ORGANIZATIONS}/ignore/receivers`, async (route) => {
        const json = MOCK_GET_RECEIVERS_IGNORE;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function goto(page: Page) {
    await page.goto(URL_DAILY_DATA, {
        waitUntil: "domcontentloaded",
    });
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
    return page.getByTestId("filter-form").getByRole("button", { name: "Reset" });
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
    await expect(page.locator(".usa-table th").nth(1)).toHaveText(/Time received/);
    await expect(page.locator(".usa-table th").nth(2)).toHaveText(/File available until/);
    await expect(page.locator(".usa-table th").nth(3)).toHaveText(/Items/);
    await expect(page.locator(".usa-table th").nth(4)).toHaveText(/Filename/);
    await expect(page.locator(".usa-table th").nth(5)).toHaveText(/Receiver/);
}
export async function detailsTableHeaders(page: Page) {
    await expect(page.locator(".usa-table th").nth(0)).toHaveText(/Facility/);
    await expect(page.locator(".usa-table th").nth(1)).toHaveText(/Location/);
    await expect(page.locator(".usa-table th").nth(2)).toHaveText(/CLIA/);
    await expect(page.locator(".usa-table th").nth(3)).toHaveText(/Total tests/);
    await expect(page.locator(".usa-table th").nth(4)).toHaveText(/Total positive/);
}

export async function setDate(page: Page, locator: string, offsetDate: number) {
    const currentDate = new Date();
    const newDate = format(currentDate.setDate(currentDate.getDate() - offsetDate), "MM/dd/yyyy");
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
