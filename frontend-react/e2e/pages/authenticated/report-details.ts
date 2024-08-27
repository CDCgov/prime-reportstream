import { expect, Page } from "@playwright/test";
import fs from "node:fs";
import { MOCK_GET_DELIVERY } from "../../mocks/delivery";
import { MOCK_GET_HISTORY_REPORT } from "../../mocks/historyReport";
import { MOCK_GET_SUBMISSION_HISTORY } from "../../mocks/submissionHistory";

export const URL_REPORT_DETAILS = "/report-details";
export const API_WATERS_REPORT = "**/api/waters/report";
export const API_HISTORY_REPORT = "**/api/history/report";
export const API_WATERS_ORG = "**/api/v1/waters/org";

export async function goto(page: Page, id: string) {
    await page.goto(`${URL_REPORT_DETAILS}/${id}`, {
        waitUntil: "domcontentloaded",
    });
}
export async function mockGetReportDeliveryResponse(page: Page, id: string, responseStatus = 200) {
    await page.route(`${API_WATERS_REPORT}/${id}/delivery`, async (route) => {
        const json = MOCK_GET_DELIVERY;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetSubmissionHistoryResponse(page: Page, id: string, responseStatus = 200) {
    await page.route(`${API_WATERS_REPORT}/${id}/history`, async (route) => {
        const json = MOCK_GET_SUBMISSION_HISTORY;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetHistoryReportResponse(page: Page, id: string, responseStatus = 200) {
    await page.route(`${API_HISTORY_REPORT}/${id}`, async (route) => {
        const json = MOCK_GET_HISTORY_REPORT;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function downloadFile(page: Page, id: string, fileName: string) {
    await mockGetHistoryReportResponse(page, id);
    const [download] = await Promise.all([
        // Start waiting for the download
        page.waitForEvent("download"),
        // Perform the action that initiates download
        await page.getByRole("button", { name: "CSV" }).click(),
    ]);

    // assert filename
    expect(download.suggestedFilename()).toBe(fileName);
    // get and assert stats
    expect((await fs.promises.stat(await download.path())).size).toBeGreaterThan(200);
}
