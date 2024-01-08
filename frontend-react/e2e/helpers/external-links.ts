import { expect, Page } from "@playwright/test";

export class ExternalLinks {
    constructor(private readonly page: Page) {}

    async clickOnConnect(locator: string, linkName: string) {
        const newTabPromise = this.page.waitForEvent("popup");
        await this.page
            .locator(locator)
            .getByRole("link", { name: linkName })
            .click();

        const newTab = await newTabPromise;
        await newTab.waitForLoadState();
        await expect(newTab).toHaveURL(
            "https://app.smartsheetgov.com/b/form/48f580abb9b440549b1a9cf996ba6957",
        );
        await expect(
            newTab.getByText("Connect with ReportStream"),
        ).toBeTruthy();
    }
}
