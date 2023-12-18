import { expect, Page } from "@playwright/test";

export class GettingStarted {
    constructor(private readonly page: Page) {}

    async onLoad() {
        await expect(this.page).toHaveURL(/getting-started/);
        await expect(this.page).toHaveTitle(/Getting started/);
    }
}
