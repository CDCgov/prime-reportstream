import { scrollToFooter, scrollToTop } from "../../../../../helpers/utils";
import { DeveloperResourcesApiDocumentationPage } from "../../../../../pages/developer-resources/api/documentation/index";
import { test as baseTest, expect } from "../../../../../test";

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
        test("has correct title", async ({ developerResourcesApiDocumentationPage }) => {
            await expect(developerResourcesApiDocumentationPage.page).toHaveTitle(
                developerResourcesApiDocumentationPage.title,
            );
            await expect(developerResourcesApiDocumentationPage.heading).toBeVisible();
        });

        test("has side nav", async ({ developerResourcesApiDocumentationPage }) => {
            await expect(
                developerResourcesApiDocumentationPage.page.getByRole("navigation", { name: "side-navigation" }),
            ).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ developerResourcesApiDocumentationPage }) => {
                await expect(developerResourcesApiDocumentationPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({
                developerResourcesApiDocumentationPage,
            }) => {
                await expect(developerResourcesApiDocumentationPage.footer).not.toBeInViewport();
                await scrollToFooter(developerResourcesApiDocumentationPage.page);
                await expect(developerResourcesApiDocumentationPage.footer).toBeInViewport();
                await expect(developerResourcesApiDocumentationPage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(developerResourcesApiDocumentationPage.page);
                await expect(developerResourcesApiDocumentationPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
