import { expect, Page } from "@playwright/test";

export class Header {
    constructor(private readonly page: Page) {}

    async clickOnHome() {
        await this.page.getByTestId("header").getByTitle("Home").click();
        await expect(this.page).toHaveURL("/");
    }

    async clickOnAbout() {
        await this.page
            .getByTestId("header")
            .getByTestId("navDropDownButton")
            .click();

        await expect(this.page.getByText("About ReportStream")).toBeTruthy();
        await expect(this.page.getByText("Our network")).toBeTruthy();
        await expect(this.page.getByText("News")).toBeTruthy();
        await expect(this.page.getByText("Case studies")).toBeTruthy();
        await expect(this.page.getByText("Security")).toBeTruthy();
        await expect(this.page.getByText("Release notes")).toBeTruthy();
    }

    async clickOnGettingStarted() {
        await this.page
            .getByTestId("header")
            .getByRole("link", { name: "Getting Started" })
            .click();

        await expect(this.page).toHaveURL(/getting-started/);
    }

    async clickOnDevelopers() {
        await this.page
            .getByTestId("header")
            .getByRole("link", { name: "Developers" })
            .click();
        await expect(this.page).toHaveURL(/.*developer-resources/);
    }

    async clickOnYourConnection() {
        await this.page
            .getByTestId("header")
            .getByRole("link", { name: "Your Connection" })
            .click();
        await expect(this.page).toHaveURL(/.*managing-your-connection/);
    }

    async clickOnSupport() {
        await this.page
            .getByTestId("header")
            .getByRole("link", { name: "Support" })
            .click();
        await expect(this.page).toHaveURL(/.*support/);
    }
}
