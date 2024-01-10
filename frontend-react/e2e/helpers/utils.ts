import fs from "node:fs";

import { Page, expect } from "@playwright/test";

export class Utils {
    constructor(private readonly page: Page) {}

    async scrollToFooter() {
        // Scrolling to the bottom of the page
        await this.page.locator("footer").scrollIntoViewIfNeeded();
    }

    async scrollToTop() {
        // Scroll to the top of the page
        await this.page.evaluate(() => window.scrollTo(0, 0));
    }

    async selectTestOrg() {
        await this.page.goto("/admin/settings");
        await expect(this.page).toHaveTitle(/Admin-Organizations/);
        await this.page.getByTestId("textInput").fill("ignore");
        await this.page.getByTestId("ignore_set").click();
    }

    /**
     * Save session storage to file. Session storage is not handled in
     * playwright's storagestate.
     */
    async saveSessionStorage(userType: string) {
        const sessionJson = await this.page.evaluate(() =>
            JSON.stringify(sessionStorage),
        );
        fs.writeFileSync(
            `playwright/.auth/${userType}-session.json`,
            sessionJson,
            "utf-8",
        );
    }

    async restoreSessionStorage(userType: string) {
        const session = JSON.parse(
            fs.readFileSync(
                `playwright/.auth/${userType}-session.json`,
                "utf-8",
            ),
        );
        await this.page.context().addInitScript((session) => {
            for (const [key, value] of Object.entries<any>(session))
                window.sessionStorage.setItem(key, value);
        }, session);
    }
}
