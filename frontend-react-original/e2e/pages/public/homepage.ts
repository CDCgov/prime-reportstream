import { BasePage, BasePageTestArgs } from "../BasePage";

export class HomePage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/",
                title: "ReportStream - CDC's free, interoperable data transfer platform",
                heading: testArgs.page.getByRole("heading", {
                    name: "CDC’s free, single connection to streamline your data transfer and improve public health",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
