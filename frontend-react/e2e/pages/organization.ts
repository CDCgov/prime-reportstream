import { Page } from "@playwright/test";

import {
    MOCK_GET_RECEIVERS_AK,
    MOCK_GET_RECEIVERS_IGNORE,
} from "../mocks/organizations";

export const API_ORGANIZATIONS = "**/api/settings/organizations";
export async function goto(page: Page) {
    await page.goto("/admin/settings", {
        waitUntil: "domcontentloaded",
    });
}

export async function mockGetOrgAlaskaReceiversResponse(
    page: Page,
    responseStatus = 200,
) {
    await page.route(`${API_ORGANIZATIONS}/ak-phd/receivers`, async (route) => {
        const json = MOCK_GET_RECEIVERS_AK;
        await route.fulfill({ json, status: responseStatus });
    });
}

export async function mockGetOrgIgnoreReceiversResponse(
    page: Page,
    responseStatus = 200,
) {
    await page.route(`${API_ORGANIZATIONS}/ignore/receivers`, async (route) => {
        const json = MOCK_GET_RECEIVERS_IGNORE;
        await route.fulfill({ json, status: responseStatus });
    });
}
