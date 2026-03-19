import { BasePage, BasePageTestArgs } from "../../../BasePage";

export class SubmissionStatusAndErrorsPage extends BasePage {
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/developer-resources/documentation/submission-status-and-errors",
                title: "Submission status and errors",
                heading: testArgs.page.getByRole("heading", {
                    name: "Submission status and errors",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}
