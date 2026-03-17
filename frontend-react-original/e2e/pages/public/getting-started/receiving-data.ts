import { BasePage, BasePageTestArgs } from "../../BasePage";

export class ReceivingDataPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/getting-started/receiving-data",
                title: "Get started receiving data with ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Get started receiving data",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
