import { BasePage, BasePageTestArgs } from "../BasePage";

export class SupportPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/support",
                title: "ReportStream support",
                heading: testArgs.page.getByRole("heading", {
                    name: "Support",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
