import { expect, Page } from "@playwright/test";
import { TEST_ORG } from "../helpers/utils";
import { MOCK_GET_REPORT_HISTORY } from "../mocks/history";
import { MOCK_GET_SUBMISSIONS } from "../mocks/submissions";

const URL_SUBMISSION_HISTORY = "/submissions";
export const API_GET_SUBMISSIONS = `**/api/waters/org/${TEST_ORG}/submissions?*`;
export const API_GET_REPORT_HISTORY = `**/api/waters/report/**`;
export async function goto(page: Page) {
    await page.goto(URL_SUBMISSION_HISTORY, {
        waitUntil: "domcontentloaded",
    });
}

export async function mockGetSubmissionsResponse(
    page: Page,
    responseStatus = 200,
) {
    await page.route(API_GET_SUBMISSIONS, async (route) => {
        const json = MOCK_GET_SUBMISSIONS;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetReportHistoryResponse(
    page: Page,
    responseStatus = 200,
) {
    await page.route(API_GET_REPORT_HISTORY, async (route) => {
        const json = MOCK_GET_REPORT_HISTORY;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function openDetailPage(page: Page, id: string) {
    const reportDetailsPage = page;
    await reportDetailsPage.waitForLoadState();
    await expect(reportDetailsPage).toHaveURL(/\/submissions\/${id}/);
    expect(
        reportDetailsPage.getByText(
            /Report ID:73e3cbc8-9920-4ab7-871f-843a1db4c074/,
        ),
    ).toBeTruthy();
}
