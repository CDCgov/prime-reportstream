import { expect, Page } from "@playwright/test";

export class OurNetwork {
    constructor(private readonly page: Page) {}

    async onLoad() {
        await expect(this.page).toHaveURL(/our-network/);
        await expect(this.page).toHaveTitle(/Our network/);
    }

    async clickOnLiveMap() {
        await this.page.getByTestId("map").click();
        await expect(this.page).toHaveURL("/about/our-network");
    }
}
