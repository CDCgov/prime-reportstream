import { BasePage, BasePageTestArgs } from "./BasePage";

export class AboutPage extends BasePage {
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
    }
}
