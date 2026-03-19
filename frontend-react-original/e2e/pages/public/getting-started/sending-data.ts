import { BasePage, BasePageTestArgs } from "../../BasePage";

export class SendingDataPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/getting-started/sending-data",
                title: "Get started sending data with ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Get started sending data",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
