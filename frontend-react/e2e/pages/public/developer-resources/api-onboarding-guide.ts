import { BasePage, BasePageTestArgs } from "../../BasePage";

export class DeveloperResourcesApiPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/api-onboarding-guide",
                title: "API onboarding guide",
                heading: testArgs.page.getByRole("heading", {
                    name: "API onboarding guide",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
