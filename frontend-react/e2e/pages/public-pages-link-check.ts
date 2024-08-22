import { BasePage, BasePageTestArgs } from "./BasePage";

export class PublicPageLinkChecker extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/",
                title: "ReportStream - CDC's free, interoperable data transfer platform",
            },
            testArgs,
        );
    }
}
