import { Page } from "@playwright/test";
import { MOCK_GET_RESEND, MOCK_GET_SEND_FAILURES } from "../../mocks/lastMilefailures";

const URL_LAST_MILE = "/admin/lastmile";
const API_GET_RESEND = "/api/adm/getresend?days_to_show=15";
export const API_GET_SEND_FAILURES = "/api/adm/getsendfailures?days_to_show=15";

export async function goto(page: Page) {
    await page.goto(URL_LAST_MILE, {
        waitUntil: "domcontentloaded",
    });
}

export async function mockGetSendFailuresResponse(page: Page, responseStatus = 200) {
    await page.route(API_GET_SEND_FAILURES, async (route) => {
        const json = MOCK_GET_SEND_FAILURES;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetResendResponse(page: Page) {
    await page.route(API_GET_RESEND, async (route) => {
        const json = MOCK_GET_RESEND;
        await route.fulfill({ json });
    });
}
