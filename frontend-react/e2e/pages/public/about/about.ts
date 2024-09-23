import { BasePage, BasePageTestArgs } from "../../BasePage";

export class AboutPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about",
                title: "About ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "About",
                }),
            },
            testArgs,
        );
    }
}
