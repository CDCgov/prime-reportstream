import { BasePage, BasePageTestArgs } from "../../../BasePage";

export class SamplePayloadsAndOutputs extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/documentation/sample-payloads-and-output",
                title: "ReportStream API sample payloads and output",
                heading: testArgs.page.getByRole("heading", {
                    name: "Sample payloads and output",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
