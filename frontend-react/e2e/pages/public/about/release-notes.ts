import { BasePage, BasePageTestArgs } from "../../BasePage";

export class AboutReleaseNotesPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/about/release-notes",
                title: "ReportStream release notes",
                heading: testArgs.page.getByRole("heading", {
                    name: "Release notes",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
