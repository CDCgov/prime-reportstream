import { expect, Page } from "@playwright/test";
import { format } from "date-fns";
import { DailyDataDetailsPage } from "./daily-data-details";
import { API_WATERS_ORG } from "./report-details";
import { RSReceiver } from "../../../src/config/endpoints/settings";
import { TEST_ORG_AK, TEST_ORG_AK_RECEIVER, TEST_ORG_IGNORE, TEST_ORG_UP_RECEIVER_UP } from "../../helpers/utils";
import {
    MOCK_GET_DELIVERIES_AK,
    MOCK_GET_DELIVERIES_AK_FILENAME,
    MOCK_GET_DELIVERIES_AK_FULL_ELR,
    MOCK_GET_DELIVERIES_AK_REPORT_ID,
    MOCK_GET_DELIVERIES_IGNORE,
    MOCK_GET_DELIVERIES_IGNORE_FILENAME,
    MOCK_GET_DELIVERIES_IGNORE_FULL_ELR,
    MOCK_GET_DELIVERIES_IGNORE_REPORT_ID,
} from "../../mocks/deliveries";
import { MOCK_GET_DELIVERY } from "../../mocks/delivery";
import { MOCK_GET_FACILITIES } from "../../mocks/facilities";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../BasePage";

export class DailyDataPage extends BasePage {
    static readonly URL_DAILY_DATA = "/daily-data";
    static readonly API_ORGANIZATIONS = "**/api/settings/organizations";
    protected _rsReceiver: RSReceiver[];

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: DailyDataPage.URL_DAILY_DATA,
                title: "Daily Data - ReportStream",
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
            // Ignore Org
            this.createMockOrgReceiversHandler("IGNORE"),
            this.createMockGetDeliveriesForOrgHandler(TEST_ORG_IGNORE, MOCK_GET_DELIVERIES_IGNORE),
            this.createMockGetDeliveriesForOrgHandler(TEST_ORG_IGNORE, MOCK_GET_DELIVERIES_IGNORE_REPORT_ID),
            this.createMockGetDeliveriesForOrgHandler(TEST_ORG_IGNORE, MOCK_GET_DELIVERIES_IGNORE_FILENAME),
            this.createMockGetDeliveriesForOrgHandler(
                TEST_ORG_IGNORE,
                MOCK_GET_DELIVERIES_IGNORE_FULL_ELR,
                TEST_ORG_UP_RECEIVER_UP,
            ),

            // Alaska Org
            this.createMockOrgReceiversHandler("AK"),
            this.createMockGetDeliveriesForOrgHandler(TEST_ORG_AK, MOCK_GET_DELIVERIES_AK),
            this.createMockGetDeliveriesForOrgHandler(TEST_ORG_AK, MOCK_GET_DELIVERIES_AK_REPORT_ID),
            this.createMockGetDeliveriesForOrgHandler(TEST_ORG_AK, MOCK_GET_DELIVERIES_AK_FILENAME),
            this.createMockGetDeliveriesForOrgHandler(
                TEST_ORG_AK,
                MOCK_GET_DELIVERIES_AK_FULL_ELR,
                TEST_ORG_AK_RECEIVER,
            ),
            this.createMockDeliveryHandler(),
            this.createMockFacilitiesHandler(),
        ]);
    }

    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.testArgs.storageState === this.testArgs.adminLogin.path;
    }

    createMockOrgReceiversHandler(organization: string): RouteHandlerFulfillEntry {
        return [
            `${DailyDataPage.API_ORGANIZATIONS}/${organization}/receivers`,
            () => {
                return {
                    json: `MOCK_GET_RECEIVERS_${organization}`,
                };
            },
        ];
    }

    createMockGetDeliveriesForOrgHandler(
        organization: string,
        mockFileName: any,
        receiver?: string,
        responseStatus = 200,
    ): RouteHandlerFulfillEntry {
        if (receiver) {
            return [
                `${API_WATERS_ORG}/${organization}.${receiver}/deliveries?*`,
                () => {
                    return {
                        json: mockFileName,
                        status: responseStatus,
                    };
                },
            ];
        } else {
            return [
                `${API_WATERS_ORG}/${organization}/deliveries?*`,
                () => {
                    return {
                        json: mockFileName,
                        status: responseStatus,
                    };
                },
            ];
        }
    }

    createMockDeliveryHandler(): RouteHandlerFulfillEntry {
        return [
            DailyDataDetailsPage.API_DELIVERY,
            () => {
                return {
                    json: MOCK_GET_DELIVERY,
                };
            },
        ];
    }

    createMockFacilitiesHandler(): RouteHandlerFulfillEntry {
        return [
            DailyDataDetailsPage.API_FACILITIES,
            () => {
                return {
                    json: MOCK_GET_FACILITIES,
                };
            },
        ];
    }
}

const URL_DAILY_DATA = "/daily-data";

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

export function filterStatus(filters: (string | undefined)[]) {
    // RowCount is not attainable with live data since it is returned from the API
    let filterStatus = ` Showing all data for: `;

    for (let i = 0; i < filters.length; i++) {
        filterStatus += filters[i];
        if (i < filters.length - 1) {
            filterStatus += ", ";
        }
    }
    return filterStatus;
}
