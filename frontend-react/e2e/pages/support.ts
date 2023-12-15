import { expect, Page } from "@playwright/test";

export class Support {
    constructor(private readonly page: Page) {}

    async onLoad() {
        await expect(this.page).toHaveURL(/support/);
        await expect(this.page).toHaveTitle(/Support/);
    }
}
