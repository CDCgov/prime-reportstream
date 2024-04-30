import { expect, Page } from "@playwright/test";
import fs from "node:fs";
import {
    MOCK_GET_DELIVERIES_AK,
    MOCK_GET_DELIVERIES_AK_5,
    MOCK_GET_DELIVERIES_AK_ELR,
    MOCK_GET_DELIVERIES_IGNORE,
    MOCK_GET_DELIVERIES_IGNORE_FILENAME,
    MOCK_GET_DELIVERIES_IGNORE_FULL_ELR,
    MOCK_GET_DELIVERIES_IGNORE_REPORT_ID,
} from "../mocks/deliveries";
import { MOCK_GET_DELIVERY } from "../mocks/delivery";
import { MOCK_GET_FACILITIES } from "../mocks/facilities";
import { MOCK_GET_HISTORY_REPORT } from "../mocks/historyReport";

export const URL_REPORT_DETAILS = "/report-details";
export const API_WATERS_REPORT = "**/api/waters/report";
export const API_HISTORY_REPORT = "**/api/history/report";
export const API_WATERS_ORG = "**/api/waters/org";
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

export async function mockGetDeliveriesForOrgAlaskaResponse(
    page: Page,
    byReportId?: boolean,
    byFileName?: boolean,
    receiver?: string,
    responseStatus = 200,
) {
    if (receiver) {
        await page.route(
            `${API_WATERS_ORG}/ak-phd.${receiver}/deliveries?*`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_AK_ELR;
                await route.fulfill({ json, status: responseStatus });
            },
        );
    } else if (byReportId) {
        await page.route(
            `${API_WATERS_ORG}/ak-phd/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&reportId=f4155156-1230-4f0a-8a50-0a0cdec5aa0e`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_AK_5;
                await route.fulfill({
                    json,
                    status: 200,
                });
            },
        );
    } else if (byFileName) {
        await page.route(
            `${API_WATERS_ORG}/ak-phd/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&fileName=ak-receiver-transform.yml-f4155156-1230-4f0a-8a50-0a0cdec5aa0e-20240423214401.hl7`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_AK_5;
                await route.fulfill({
                    json,
                    status: 200,
                });
            },
        );
    } else {
        await page.route(
            `${API_WATERS_ORG}/ak-phd/deliveries?*`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_AK;
                await route.fulfill({ json, status: responseStatus });
            },
        );
    }
}

export async function mockGetDeliveriesForOrgIgnoreResponse(
    page: Page,
    byReportId?: boolean,
    byFileName?: boolean,
    receiver?: string,
    responseStatus = 200,
) {
    if (receiver) {
        await page.route(
            `${API_WATERS_ORG}/ignore.${receiver}/deliveries?*`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_IGNORE_FULL_ELR;
                await route.fulfill({ json, status: responseStatus });
            },
        );
    } else if (byReportId) {
        await page.route(
            `${API_WATERS_ORG}/ignore/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&reportId=729158ce-4125-46fa-bea0-3c0f910f472c`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_IGNORE_REPORT_ID;
                await route.fulfill({
                    json,
                    status: 200,
                });
            },
        );
    } else if (byFileName) {
        await page.route(
            `${API_WATERS_ORG}/ignore/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&fileName=21c217a4-d098-494c-9364-f4dcf16b1d63-20240426204235.fhir`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_IGNORE_FILENAME;
                await route.fulfill({
                    json,
                    status: 200,
                });
            },
        );
    } else {
        await page.route(
            `${API_WATERS_ORG}/ignore/deliveries?*`,
            async (route) => {
                const json = MOCK_GET_DELIVERIES_IGNORE;
                await route.fulfill({ json, status: responseStatus });
            },
        );
    }
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
