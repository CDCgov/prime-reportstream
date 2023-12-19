import { Page } from "@playwright/test";

export class Utils {
    constructor(private readonly page: Page) {}

    async scrollToFooter() {
        // Scrolling to the bottom of the page
        await this.page.locator("footer").click();
    }

    async scrollToTop() {
        // Scroll to the top of the page
        await this.page.evaluate(() => window.scrollTo(0, 0));
    }
}
