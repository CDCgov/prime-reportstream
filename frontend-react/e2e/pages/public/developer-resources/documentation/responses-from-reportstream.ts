import { BasePage, BasePageTestArgs } from "../../../BasePage";

export class ResponsesFromReportStreamPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/documentation/responses-from-reportstream",
                title: "API responses from ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Responses from ReportStream",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
