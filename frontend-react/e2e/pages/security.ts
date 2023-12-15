import { expect, Page } from "@playwright/test";

export class Security {
    constructor(private readonly page: Page) {}

    async onLoad() {
        await expect(this.page).toHaveURL(/security/);
        await expect(this.page).toHaveTitle(/Security/);
    }
}
