import { BasePage, BasePageTestArgs } from "../../BasePage";

export class AboutCaseStudiesPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about/case-studies",
                title: "ReportStream case studies",
                heading: testArgs.page.getByRole("heading", {
                    name: "Case studies",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
