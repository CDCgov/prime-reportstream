import { developerResourcesApiSideNav } from "../../../../../../helpers/internal-links";
import { SubmissionStatusAndErrorsPage } from "../../../../../../pages/public/developer-resources/documentation/submission-status-and-errors";
import { test as baseTest } from "../../../../../../test";

export interface SecurityPageFixtures {
    submissionStatusAndErrorsPage: SubmissionStatusAndErrorsPage;
}

const test = baseTest.extend<SecurityPageFixtures>({
    submissionStatusAndErrorsPage: async (
        {
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        },
        use,
    ) => {
        const page = new SubmissionStatusAndErrorsPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        });
        await page.goto();
        await use(page);
    },
});

test.describe(
    "Developer Resources / API / Documentation / Submission Status And Errors Page",
    {
        tag: "@smoke",
    },
    () => {
        test("has side nav", async ({ submissionStatusAndErrorsPage }) => {
            await submissionStatusAndErrorsPage.testSidenav(developerResourcesApiSideNav);
        });

        test("has correct title + heading", async ({ submissionStatusAndErrorsPage }) => {
            await submissionStatusAndErrorsPage.testHeader();
        });

        test("footer", async ({ submissionStatusAndErrorsPage }) => {
            await submissionStatusAndErrorsPage.testFooter();
        });
    },
);
