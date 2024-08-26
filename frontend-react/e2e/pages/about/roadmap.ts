import { BasePage, BasePageTestArgs } from "../BasePage";

export class AboutRoadmapPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about/roadmap",
                title: "ReportStream roadmap",
                heading: testArgs.page.getByRole("heading", {
                    name: "Roadmap",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
