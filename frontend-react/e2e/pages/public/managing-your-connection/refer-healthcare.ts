import { BasePage, BasePageTestArgs } from "../../BasePage";

export class ReferHealthcarePage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/managing-your-connection/refer-healthcare-organizations",
                title: "Refer health care organizations to ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Refer healthcare organizations",
                }),
            },
            testArgs,
        );
    }
}
