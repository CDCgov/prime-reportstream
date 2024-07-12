import { expect, Page } from "@playwright/test";
import {
    BasePage,
    BasePageTestArgs,
    type RouteHandlerFulfillEntry,
} from "./BasePage";
import { RSDelivery } from "../../src/config/endpoints/deliveries";
import { TEST_ORG_IGNORE } from "../helpers/utils";
import { MOCK_GET_SUBMISSION_HISTORY } from "../mocks/submissionHistory";
import { MOCK_GET_SUBMISSIONS } from "../mocks/submissions";

export const URL_SUBMISSION_HISTORY = "/submissions";
export const API_GET_REPORT_HISTORY = `**/api/waters/report/**`;

export class SubmissionHistoryPage extends BasePage {
    static readonly API_SUBMISSIONS = `**/api/waters/org/${TEST_ORG_IGNORE}/submissions?*`;
    static readonly API_GET_REPORT_HISTORY = `**/api/waters/report/**`;
    protected _submissions: RSDelivery[];
    protected _submissionHistory: RSDelivery;

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: URL_SUBMISSION_HISTORY,
                title: "ReportStream - CDC's free, interoperable data transfer platform",
                heading: testArgs.page.getByRole("heading", {
                    name: "Daily Data Details",
                }),
            },
            testArgs,
        );

        this._submissions = [];
        this._submissionHistory = {
            batchReadyAt: "",
            deliveryId: 0,
            expires: "",
            fileName: "",
            fileType: "",
            receiver: "",
            reportId: "",
            reportItemCount: 0,
            topic: "",
        };
        this.addResponseHandlers([
            [
                SubmissionHistoryPage.API_SUBMISSIONS,
                async (res) => (this._submissions = await res.json()),
            ],
            [
                SubmissionHistoryPage.API_GET_REPORT_HISTORY,
                async (res) => (this._submissionHistory = await res.json()),
            ],
        ]);
        this.addMockRouteHandlers([
            this.createMockSubmissionsHandler(),
            this.createMockSubmissionHistoryHandler(),
        ]);
    }

    createMockSubmissionsHandler(): RouteHandlerFulfillEntry {
        return [
            SubmissionHistoryPage.API_SUBMISSIONS,
            () => {
                return {
                    json: MOCK_GET_SUBMISSIONS,
                };
            },
        ];
    }

    createMockSubmissionHistoryHandler(): RouteHandlerFulfillEntry {
        return [
            SubmissionHistoryPage.API_GET_REPORT_HISTORY,
            () => {
                return {
                    json: MOCK_GET_SUBMISSION_HISTORY,
                };
            },
        ];
    }
}

export async function gotoDetails(page: Page, id: string) {
    await page.goto(`${URL_SUBMISSION_HISTORY}/${id}`, {
        waitUntil: "domcontentloaded",
    });
}

export async function mockGetReportHistoryResponse(
    page: Page,
    responseStatus = 200,
) {
    await page.route(API_GET_REPORT_HISTORY, async (route) => {
        const json = MOCK_GET_SUBMISSION_HISTORY;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function openReportIdDetailPage(page: Page, id: string) {
    await expect(page).toHaveURL(`/submissions/${id}`);
    await expect(page.getByText(`Details: ${id}`)).toBeVisible();
}

export async function title(page: Page) {
    await expect(page).toHaveTitle(
        /ReportStream - CDC's free, interoperable data transfer platform/,
    );
}

export async function tableHeaders(page: Page) {
    await expect(page.locator(".usa-table th").nth(0)).toHaveText(/Report ID/);
    await expect(page.locator(".usa-table th").nth(1)).toHaveText(
        "Date/time submitted",
    );
    await expect(page.locator(".usa-table th").nth(2)).toHaveText(/File/);
    await expect(page.locator(".usa-table th").nth(3)).toHaveText(/Records/);
    await expect(page.locator(".usa-table th").nth(4)).toHaveText(/Status/);
}

export async function breadcrumbLink(
    page: Page,
    index: number,
    linkName: string,
    expectedUrl: string,
) {
    const breadcrumbLinks = page.locator(".usa-breadcrumb ol li");
    await expect(breadcrumbLinks.nth(index)).toHaveText(linkName);
    await breadcrumbLinks
        .nth(index)
        .getByRole("link", { name: linkName })
        .click();
    await expect(page.locator("h1")).toBeAttached();
    await expect(page).toHaveURL(expectedUrl);
}
