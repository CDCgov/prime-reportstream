import { expect, Page } from "@playwright/test";
import { BasePage, BasePageTestArgs } from "../../BasePage";

export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/managing-your-connection/);
    await expect(page).toHaveTitle(/Managing your connection/);
}

export class ManagingYourConnectionPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/managing-your-connection",
                title: "Managing your connection with ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Managing Your Connection",
                }),
            },
            testArgs,
        );
    }
}
