import { Page } from "@playwright/test";

export const API_RECEIVER_STATUS = "**/api/adm/listreceiversconnstatus?*";
export async function goto(page: Page) {
    await page.goto("/admin/send-dash", {
        waitUntil: "domcontentloaded",
    });
}

export async function overridePageDate(page: Page, fakeNow: Date) {
    await page.addInitScript((fakeNow) => {
        const __Date = window.Date;
        (window.Date as any) = class extends __Date {
            constructor(...args: []) {
                if (args.length === 0) {
                    super(fakeNow);
                } else {
                    super(...args);
                }
            }
        }
    }, fakeNow.toISOString())
}
