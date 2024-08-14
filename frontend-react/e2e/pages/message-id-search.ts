import { Page } from "@playwright/test";

export const URL_MESSAGE_ID_SEARCH = "/admin/message-tracker";
export const API_MESSAGES = "**/api/messages?messageId=*";
export const API_MESSAGE = "**/api/message/*";

export const MESSAGE_ID = "582098";

export async function goto(page: Page) {
    await page.goto(URL_MESSAGE_ID_SEARCH, {
        waitUntil: "domcontentloaded",
    });
}
