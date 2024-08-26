import { BasePage, BasePageTestArgs } from "../../BasePage";

export class DeveloperResourcesApiPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/api",
                title: "Guide to connecting with ReportStream's API",
                heading: testArgs.page.getByRole("heading", {
                    name: "ReportStream API",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
