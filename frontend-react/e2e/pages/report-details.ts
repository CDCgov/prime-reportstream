import { expect, Page } from "@playwright/test";
import fs from "node:fs";
import { MOCK_GET_DELIVERY } from "../mocks/delivery";
import { MOCK_GET_FACILITIES } from "../mocks/facilities";
import { MOCK_GET_HISTORY_REPORT } from "../mocks/historyReport";

export const URL_REPORT_DETAILS = "/report-details";
export const API_WATERS_REPORT = "/api/waters/report";
export const API_HISTORY_REPORT = "/api/history/report";
export async function goto(page: Page, id: string) {
    await page.goto(`${URL_REPORT_DETAILS}/${id}`, {
        waitUntil: "domcontentloaded",
    });
}

export async function reportIdDetailPage(page: Page) {
    const reportDetailsPage = page;
    await reportDetailsPage.waitForLoadState();
    await expect(reportDetailsPage.locator("h1")).toBeAttached();
    return reportDetailsPage;
}

export async function mockGetDeliveryResponse(
    page: Page,
    id: string,
    responseStatus = 200,
) {
    await page.route(`${API_WATERS_REPORT}/${id}/delivery`, async (route) => {
        const json = MOCK_GET_DELIVERY;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetFacilitiesResponse(
    page: Page,
    id: string,
    responseStatus = 200,
) {
    await page.route(`${API_WATERS_REPORT}/${id}/facilities`, async (route) => {
        const json = MOCK_GET_FACILITIES;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetHistoryReportResponse(
    page: Page,
    id: string,
    responseStatus = 200,
) {
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
    expect(
        (await fs.promises.stat(await download.path())).size,
    ).toBeGreaterThan(200);
}
