import { expect, Page } from "@playwright/test";

export class OurNetwork {
    constructor(private readonly page: Page) {}

    async clickOnLiveMap() {
        await this.page.getByTestId("map").click();
        await expect(this.page).toHaveURL("/about/our-network");
    }
}
