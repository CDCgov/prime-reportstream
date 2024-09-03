import { expect, Page } from "@playwright/test";
import { TEST_ORG_IGNORE } from "../../helpers/utils";
import { MOCK_GET_SUBMISSION_HISTORY } from "../../mocks/submissionHistory";
import { MOCK_GET_SUBMISSIONS } from "../../mocks/submissions";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../BasePage";

export const URL_SUBMISSION_HISTORY = "/submissions";
export const API_GET_REPORT_HISTORY = `**/api/waters/report/**`;
export const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";

export class SubmissionHistoryPage extends BasePage {
    static readonly URL_SUBMISSION_HISTORY = "/submissions";

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: SubmissionHistoryPage.URL_SUBMISSION_HISTORY,
                title: "ReportStream - CDC's free, interoperable data transfer platform",
                heading: testArgs.page.getByRole("heading", {
                    name: "Submission history",
                }),
            },
            testArgs,
        );

        this.addMockRouteHandlers([
            // Ignore Org
            this.createMockSubmissionsForOrgHandler(TEST_ORG_IGNORE, MOCK_GET_SUBMISSIONS),
            this.createMockSubmissionHistoryHandler(),
        ]);
    }

    createMockSubmissionsForOrgHandler(
        organization: string,
        mockFileName: any,
        responseStatus = 200,
    ): RouteHandlerFulfillEntry {
        return [
            `**/api/waters/org/${organization}/submissions?*`,
            () => {
                return {
                    json: mockFileName,
                    status: responseStatus,
                };
            },
        ];
    }

    createMockSubmissionHistoryHandler(responseStatus = 200): RouteHandlerFulfillEntry {
        return [
            API_GET_REPORT_HISTORY,
            () => {
                return {
                    json: MOCK_GET_SUBMISSION_HISTORY,
                    status: responseStatus,
                };
            },
        ];
    }

    get filterButton() {
        return this.page.getByRole("button", {
            name: "Filter",
        });
    }

    get clearButton() {
        return this.page.getByRole("button", {
            name: "Clear",
        });
    }

    /**
     * Error expected additionally if user context isn't admin
     */
    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.testArgs.storageState === this.testArgs.adminLogin.path;
    }
}

export async function goto(page: Page) {
    await page.goto(URL_SUBMISSION_HISTORY, {
        waitUntil: "domcontentloaded",
    });
}

export async function mockGetReportHistoryResponse(page: Page, responseStatus = 200) {
    await page.route(API_GET_REPORT_HISTORY, async (route) => {
        const json = MOCK_GET_SUBMISSION_HISTORY;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function openReportIdDetailPage(page: Page, id: string) {
    await expect(page).toHaveURL(`/submissions/${id}`);
    await expect(page.getByText(`Details: ${id}`)).toBeVisible();
}

export async function tableHeaders(page: Page) {
    await expect(page.locator(".usa-table th").nth(0)).toHaveText(/Report ID/);
    await expect(page.locator(".usa-table th").nth(1)).toHaveText("Date/time submitted");
    await expect(page.locator(".usa-table th").nth(2)).toHaveText(/File/);
    await expect(page.locator(".usa-table th").nth(3)).toHaveText(/Records/);
    await expect(page.locator(".usa-table th").nth(4)).toHaveText(/Status/);
}

export async function breadcrumbLink(page: Page, index: number, linkName: string, expectedUrl: string) {
    const breadcrumbLinks = page.locator(".usa-breadcrumb ol li");
    await expect(breadcrumbLinks.nth(index)).toHaveText(linkName);
    await breadcrumbLinks.nth(index).getByRole("link", { name: linkName }).click();
    await expect(page.locator("h1")).toBeAttached();
    await expect(page).toHaveURL(expectedUrl);
}
