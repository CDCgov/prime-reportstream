import { expect, Page } from "@playwright/test";
import { MOCK_GET_REPORT_HISTORY } from "../mocks/history";
import { MOCK_GET_SUBMISSIONS } from "../mocks/submissions";

export const URL_SUBMISSION_HISTORY = "/submissions";
export const API_GET_REPORT_HISTORY = `**/api/waters/report/**`;
export async function goto(page: Page) {
    await page.goto(URL_SUBMISSION_HISTORY, {
        waitUntil: "domcontentloaded",
    });
}

export function getOrgSubmissionsAPI(org: string) {
    return `**/api/waters/org/${org}/submissions?*`;
}

export async function mockGetSubmissionsResponse(
    page: Page,
    org: string,
    responseStatus = 200,
) {
    const submissionsApi = getOrgSubmissionsAPI(org);
    await page.route(submissionsApi, async (route) => {
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

export async function openReportIdDetailPage(page: Page, id: string) {
    await expect(page).toHaveURL(`/submissions/${id}`);
    await expect(page.getByText(`Details: ${id}`)).toBeVisible();
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
