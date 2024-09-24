import { BasePage, BasePageTestArgs } from "../../../BasePage";

export class DeveloperResourcesApiGettingStartedPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/api/getting-started",
                title: "Getting started with ReportStream's API",
                heading: testArgs.page.getByRole("heading", {
                    name: "Getting started",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
