import { expect, Page } from "@playwright/test";
import fs from "node:fs";
import {
    MOCK_GET_DELIVERIES_AK,
    MOCK_GET_DELIVERIES_AK_FILENAME,
    MOCK_GET_DELIVERIES_AK_FULL_ELR,
    MOCK_GET_DELIVERIES_AK_REPORT_ID,
    MOCK_GET_DELIVERIES_IGNORE,
    MOCK_GET_DELIVERIES_IGNORE_FILENAME,
    MOCK_GET_DELIVERIES_IGNORE_FULL_ELR,
    MOCK_GET_DELIVERIES_IGNORE_REPORT_ID,
} from "../mocks/deliveries";
import { MOCK_GET_DELIVERY } from "../mocks/delivery";
import { MOCK_GET_FACILITIES } from "../mocks/facilities";
import { MOCK_GET_HISTORY_REPORT } from "../mocks/historyReport";
import { MOCK_GET_SUBMISSION_HISTORY } from "../mocks/submissionHistory";

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

interface MockGetDeliveriesForOrgResponseOptions {
    page: Page;
    byReportId?: boolean;
    byFileName?: boolean;
    receiver?: string;
    responseStatus?: number;
}

export async function mockGetDeliveriesForOrgAlaskaResponse({
    page,
    byReportId,
    byFileName,
    receiver,
    responseStatus = 200,
}: MockGetDeliveriesForOrgResponseOptions) {
    if (receiver) {
        await page.route(`${API_WATERS_ORG}/ak-phd.${receiver}/deliveries?*`, async (route) => {
            await route.fulfill({ json: MOCK_GET_DELIVERIES_AK_FULL_ELR, status: responseStatus });
        });
    } else if (byReportId) {
        await page.route(
            `${API_WATERS_ORG}/ak-phd/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&reportId=f4155156-1230-4f0a-8a50-0a0cdec5aa0e`,
            async (route) => {
                await route.fulfill({
                    json: MOCK_GET_DELIVERIES_AK_REPORT_ID,
                    status: 200,
                });
            },
        );
    } else if (byFileName) {
        await page.route(
            `${API_WATERS_ORG}/ak-phd/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&fileName=ak-receiver-transform.yml-f4155156-1230-4f0a-8a50-0a0cdec5aa0e-20240423214401.hl7`,
            async (route) => {
                await route.fulfill({
                    json: MOCK_GET_DELIVERIES_AK_FILENAME,
                    status: 200,
                });
            },
        );
    } else {
        await page.route(`${API_WATERS_ORG}/ak-phd/deliveries?*`, async (route) => {
            await route.fulfill({ json: MOCK_GET_DELIVERIES_AK, status: responseStatus });
        });
    }
}

export async function mockGetDeliveriesForOrgIgnoreResponse({
    page,
    byReportId,
    byFileName,
    receiver,
    responseStatus = 200,
}: MockGetDeliveriesForOrgResponseOptions) {
    if (receiver) {
        await page.route(`${API_WATERS_ORG}/ignore.${receiver}/deliveries?*`, async (route) => {
            await route.fulfill({ json: MOCK_GET_DELIVERIES_IGNORE_FULL_ELR, status: responseStatus });
        });
    } else if (byReportId) {
        await page.route(
            `${API_WATERS_ORG}/ignore/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&reportId=729158ce-4125-46fa-bea0-3c0f910f472c`,
            async (route) => {
                await route.fulfill({
                    json: MOCK_GET_DELIVERIES_IGNORE_REPORT_ID,
                    status: 200,
                });
            },
        );
    } else if (byFileName) {
        await page.route(
            `${API_WATERS_ORG}/ignore/deliveries?sortdir=DESC&cursor=3000-01-01T00:00:00.000Z&since=2000-01-01T00:00:00.000Z&until=3000-01-01T00:00:00.000Z&pageSize=61&receivingOrgSvcStatus=ACTIVE,TESTING&fileName=21c217a4-d098-494c-9364-f4dcf16b1d63-20240426204235.fhir`,
            async (route) => {
                await route.fulfill({
                    json: MOCK_GET_DELIVERIES_IGNORE_FILENAME,
                    status: 200,
                });
            },
        );
    } else {
        await page.route(`${API_WATERS_ORG}/ignore/deliveries?*`, async (route) => {
            await route.fulfill({ json: MOCK_GET_DELIVERIES_IGNORE, status: responseStatus });
        });
    }
}

export async function mockGetReportFacilitiesResponse(page: Page, id: string, responseStatus = 200) {
    await page.route(`${API_WATERS_REPORT}/${id}/facilities`, async (route) => {
        const json = MOCK_GET_FACILITIES;
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
