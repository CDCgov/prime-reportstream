import { developerResourcesApiSideNav } from "../../../../../../helpers/internal-links";
import { DeveloperResourcesApiDocumentationPage } from "../../../../../../pages/public/developer-resources/documentation/index";
import { test as baseTest } from "../../../../../../test";

export interface SecurityPageFixtures {
    developerResourcesApiDocumentationPage: DeveloperResourcesApiDocumentationPage;
}

const test = baseTest.extend<SecurityPageFixtures>({
    developerResourcesApiDocumentationPage: async (
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
        const page = new DeveloperResourcesApiDocumentationPage({
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
    "Developer Resources / API / Documentation / Index page",
    {
        tag: "@smoke",
    },
    () => {
        test("has side nav", async ({ developerResourcesApiDocumentationPage }) => {
            await developerResourcesApiDocumentationPage.testSidenav(developerResourcesApiSideNav);
        });

        test("has correct title + heading", async ({ developerResourcesApiDocumentationPage }) => {
            await developerResourcesApiDocumentationPage.testHeader();
        });

        test("footer", async ({ developerResourcesApiDocumentationPage }) => {
            await developerResourcesApiDocumentationPage.testFooter();
        });
    },
);
