import { BasePage, BasePageTestArgs } from "../../../BasePage";

export class DeveloperResourcesApiDocumentationPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/api/documentation",
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
