import { expect, Page } from "@playwright/test";
import fs from "node:fs";

export async function scrollToFooter(page: Page) {
    // Scrolling to the bottom of the page
    await page.locator("footer").scrollIntoViewIfNeeded();
}

export async function scrollToTop(page: Page) {
    // Scroll to the top of the page
    await page.evaluate(() => window.scrollTo(0, 0));
}

export async function selectTestOrg(page: Page) {
    await page.goto("/admin/settings");
    await expect(page).toHaveTitle(/Admin-Organizations/);
    await page.getByTestId("textInput").fill("ignore");
    await page.getByTestId("ignore_set").click();
}

/**
 * Save session storage to file. Session storage is not handled in
 * playwright's storagestate.
 */
export async function saveSessionStorage(userType: string, page: Page) {
    const sessionJson = await page.evaluate(() =>
        JSON.stringify(sessionStorage),
    );
    fs.writeFileSync(
        `playwright/.auth/${userType}-session.json`,
        sessionJson,
        "utf-8",
    );
}

export async function restoreSessionStorage(userType: string, page: Page) {
    const session = JSON.parse(
        fs.readFileSync(`playwright/.auth/${userType}-session.json`, "utf-8"),
    );
    await page.context().addInitScript((session) => {
        for (const [key, value] of Object.entries<any>(session))
            window.sessionStorage.setItem(key, value);
    }, session);
}
