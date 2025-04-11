import { developerResourcesApiSideNav } from "../../../../../../helpers/internal-links";
import { ResponsesFromReportStreamPage } from "../../../../../../pages/public/developer-resources/documentation/responses-from-reportstream";
import { test as baseTest } from "../../../../../../test";

export interface SecurityPageFixtures {
    responsesFromReportStreamPage: ResponsesFromReportStreamPage;
}

const test = baseTest.extend<SecurityPageFixtures>({
    responsesFromReportStreamPage: async (
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
        const page = new ResponsesFromReportStreamPage({
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
    "Developer Resources / API / Documentation / Responses From ReportStream page",
    {
        tag: "@smoke",
    },
    () => {
        test("has side nav", async ({ responsesFromReportStreamPage }) => {
            await responsesFromReportStreamPage.testSidenav(developerResourcesApiSideNav);
        });

        test("has correct title + heading", async ({ responsesFromReportStreamPage }) => {
            await responsesFromReportStreamPage.testHeader();
        });

        test("footer", async ({ responsesFromReportStreamPage }) => {
            await responsesFromReportStreamPage.testFooter();
        });
    },
);
