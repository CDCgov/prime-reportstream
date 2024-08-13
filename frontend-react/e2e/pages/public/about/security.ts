import { expect, Page } from "@playwright/test";
import { BasePage, BasePageTestArgs } from "../../BasePage";

export class SecurityPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about/security",
                title: "ReportStream security",
                heading: testArgs.page.getByRole("heading", {
                    name: "Security",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}

export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/security/);
    await expect(page).toHaveTitle(/ReportStream security/);
}
