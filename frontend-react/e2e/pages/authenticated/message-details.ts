import { Page } from "@playwright/test";

import { MESSAGE_ID } from "../../pages/authenticated/message-id-search";

export const URL_MESSAGE_DETAILS = `/message-details/${MESSAGE_ID}`;

export async function goto(page: Page) {
    await page.goto(URL_MESSAGE_DETAILS, {
        waitUntil: "domcontentloaded",
    });
}
