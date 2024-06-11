import { Locator } from "@playwright/test";
import { BasePage, BasePageTestArgs } from "./BasePage";

export class AboutPage extends BasePage {
    readonly footer: Locator;
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about",
                title: "About",
                heading: testArgs.page.getByRole("heading", {
                    name: "About",
                }),
            },
            testArgs,
        );
        this.footer = this.page.locator("footer");
    }
}
