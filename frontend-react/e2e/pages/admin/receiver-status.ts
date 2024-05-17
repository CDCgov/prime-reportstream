import { Page } from "@playwright/test";
import { startOfDay, subDays } from "date-fns";

export const API_RECEIVER_STATUS = "**/api/adm/listreceiversconnstatus?*";
export async function goto(page: Page) {
    await page.goto("/admin/send-dash", {
        waitUntil: "domcontentloaded",
    });
}

/**
 * Get date through browser as local date behavior differs
 */
export async function createNewBrowserDate(page: Page) {
    const browserDate = await page.evaluate<string>(
        "new Date().toLocaleString()",
    );
    return new Date(browserDate);
}

export async function getDateStrings(page: Page) {
    const endDate = await createNewBrowserDate(page);
    const startDate = subDays(endDate, 2)
    const priorDayDate = subDays(endDate, 1)
    const endDateString = startOfDay(endDate).toLocaleDateString()
    const startDateString = startOfDay(startDate).toLocaleDateString()
    const priorDayString = startOfDay(priorDayDate).toLocaleDateString()

    const result = {
        endDate,
        startDate,
        priorDayDate,
        endDateString,
        startDateString,
        priorDayString
    }

    return result
}