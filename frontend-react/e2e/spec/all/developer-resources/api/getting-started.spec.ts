import { scrollToFooter, scrollToTop } from "../../../../helpers/utils";
import { DeveloperResourcesApiGettingStartedPage } from "../../../../pages/developer-resources/api/getting-started";
import { test as baseTest, expect } from "../../../../test";

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
        test("has correct title", async ({ developerResourcesApiGettingStartedPage }) => {
            await expect(developerResourcesApiGettingStartedPage.page).toHaveTitle(
                developerResourcesApiGettingStartedPage.title,
            );
            await expect(developerResourcesApiGettingStartedPage.heading).toBeVisible();
        });

        test("has side nav", async ({ developerResourcesApiGettingStartedPage }) => {
            await expect(
                developerResourcesApiGettingStartedPage.page.getByRole("navigation", { name: "side-navigation" }),
            ).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ developerResourcesApiGettingStartedPage }) => {
                await expect(developerResourcesApiGettingStartedPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({
                developerResourcesApiGettingStartedPage,
            }) => {
                await expect(developerResourcesApiGettingStartedPage.footer).not.toBeInViewport();
                await scrollToFooter(developerResourcesApiGettingStartedPage.page);
                await expect(developerResourcesApiGettingStartedPage.footer).toBeInViewport();
                await expect(
                    developerResourcesApiGettingStartedPage.page.getByTestId("govBanner"),
                ).not.toBeInViewport();
                await scrollToTop(developerResourcesApiGettingStartedPage.page);
                await expect(developerResourcesApiGettingStartedPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
