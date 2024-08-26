import { BasePage, BasePageTestArgs } from "../BasePage";

export class DeveloperResourcesIndexPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources",
                title: "ReportStream developer resources",
                heading: testArgs.page.getByRole("heading", {
                    name: "Developer resources",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
