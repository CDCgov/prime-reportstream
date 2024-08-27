import { DeveloperResourcesApiGettingStartedPage } from "../../../../../pages/public/developer-resources/api/getting-started";
import { test as baseTest } from "../../../../../test";

export interface SecurityPageFixtures {
    developerResourcesApiGettingStartedPage: DeveloperResourcesApiGettingStartedPage;
}

const test = baseTest.extend<SecurityPageFixtures>({
    developerResourcesApiGettingStartedPage: async (
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
        const page = new DeveloperResourcesApiGettingStartedPage({
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
    "Developer Resources / API / Getting started page",
    {
        tag: "@smoke",
    },
    () => {
        test("has side nav", async ({ developerResourcesApiGettingStartedPage }) => {
            await developerResourcesApiGettingStartedPage.testSidenav([]);
        });

        test("has correct title + heading", async ({ developerResourcesApiGettingStartedPage }) => {
            await developerResourcesApiGettingStartedPage.testHeader();
        });

        test("footer", async ({ developerResourcesApiGettingStartedPage }) => {
            await developerResourcesApiGettingStartedPage.testFooter();
        });
    },
);
