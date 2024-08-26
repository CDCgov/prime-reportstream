import { BasePage, BasePageTestArgs } from "../../../BasePage";

export class SamplePayloadsAndOutputs extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/api/documentation/sample-payloads-and-outputs",
                title: "ReportStream API sample payloads and outputs",
                heading: testArgs.page.getByRole("heading", {
                    name: "Sample payloads and outputs",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
