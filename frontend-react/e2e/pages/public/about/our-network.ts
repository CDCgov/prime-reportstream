import { BasePage, BasePageTestArgs } from "../../BasePage";

export class OurNetworkPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about/our-network",
                title: "Our network - ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Our network",
                }),
            },
            testArgs,
        );
    }
}
