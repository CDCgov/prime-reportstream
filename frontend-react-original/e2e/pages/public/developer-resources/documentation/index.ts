import { BasePage, BasePageTestArgs } from "../../../BasePage";

export class DeveloperResourcesApiDocumentationPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/documentation",
                title: "ReportStream API documentation",
                heading: testArgs.page.getByRole("heading", {
                    name: "Documentation",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
