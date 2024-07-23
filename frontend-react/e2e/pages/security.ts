import { BasePage, BasePageTestArgs } from "./BasePage";

export class SecurityPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about/security",
                title: "ReportStream security",
                heading: testArgs.page.getByRole("heading", {
                    name: "Security",
                }),
            },
            testArgs,
        );
    }
}
