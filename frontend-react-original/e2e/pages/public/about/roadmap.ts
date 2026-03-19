import { BasePage, BasePageTestArgs } from "../../BasePage";

export class RoadmapPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about/roadmap",
                title: "Product roadmap",
                heading: testArgs.page.getByRole("heading", {
                    name: "Product roadmap",
                }),
            },
            testArgs,
        );
    }
}
