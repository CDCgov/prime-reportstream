import { scrollToFooter, scrollToTop } from "../../../helpers/utils";
import { DeveloperResourcesIndexPage } from "../../../pages/developer-resources/index";
import { test as baseTest, expect } from "../../../test";

export interface Fixtures {
    developerResourcesIndexPage: DeveloperResourcesIndexPage;
}

const test = baseTest.extend<Fixtures>({
    developerResourcesIndexPage: async (
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
        const page = new DeveloperResourcesIndexPage({
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
    "Developer Resources / Index page",
    {
        tag: "@smoke",
    },
    () => {
        test("has correct title", async ({ developerResourcesIndexPage }) => {
            await expect(developerResourcesIndexPage.page).toHaveTitle(developerResourcesIndexPage.title);
            await expect(developerResourcesIndexPage.heading).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ developerResourcesIndexPage }) => {
                await expect(developerResourcesIndexPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ developerResourcesIndexPage }) => {
                await expect(developerResourcesIndexPage.footer).not.toBeInViewport();
                await scrollToFooter(developerResourcesIndexPage.page);
                await expect(developerResourcesIndexPage.footer).toBeInViewport();
                await expect(developerResourcesIndexPage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(developerResourcesIndexPage.page);
                await expect(developerResourcesIndexPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
