import { developerResourcesApiSideNav } from "../../../../../helpers/internal-links";
import { DeveloperResourcesApiPage } from "../../../../../pages/public/developer-resources/api-onboarding-guide";
import { test as baseTest } from "../../../../../test.js";

export interface Fixtures {
    developerResourcesApiPage: DeveloperResourcesApiPage;
}

const test = baseTest.extend<Fixtures>({
    developerResourcesApiPage: async (
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
        const page = new DeveloperResourcesApiPage({
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
    "Developer Resources / API page",
    {
        tag: "@smoke",
    },
    () => {
        test("has side nav", async ({ developerResourcesApiPage }) => {
            await developerResourcesApiPage.testSidenav(developerResourcesApiSideNav);
        });

        test("has correct title + heading", async ({ developerResourcesApiPage }) => {
            await developerResourcesApiPage.testHeader();
        });

        test("footer", async ({ developerResourcesApiPage }) => {
            await developerResourcesApiPage.testFooter();
        });
    },
);
