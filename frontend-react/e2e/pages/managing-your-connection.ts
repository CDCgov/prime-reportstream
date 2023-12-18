import { expect, Page } from "@playwright/test";

export class ManagingYourConnection {
    constructor(private readonly page: Page) {}

    async onLoad() {
        await expect(this.page).toHaveURL(/managing-your-connection/);
        await expect(this.page).toHaveTitle(/Managing your connection/);
    }
}
