import { expect, Page } from "@playwright/test";

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
        await this.page.getByTestId("ca-dph_set").click();
    }
}
