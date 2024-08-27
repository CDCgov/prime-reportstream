import { scrollToFooter, scrollToTop } from "../../../../helpers/utils.js";
import { DeveloperResourcesApiPage } from "../../../../pages/developer-resources/api/api.js";
import { test as baseTest, expect } from "../../../../test.js";

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
        test("has correct title", async ({ developerResourcesApiPage }) => {
            await expect(developerResourcesApiPage.page).toHaveTitle(developerResourcesApiPage.title);
            await expect(developerResourcesApiPage.heading).toBeVisible();
        });

        test("has side nav", async ({ developerResourcesApiPage }) => {
            await expect(
                developerResourcesApiPage.page.getByRole("navigation", { name: "side-navigation " }),
            ).toBeVisible();
        });

        test("pdf file download works", async ({ developerResourcesApiPage }) => {
            const downloadPromise = developerResourcesApiPage.page.waitForEvent("download");
            await developerResourcesApiPage.page.getByRole("link", { name: "downloadable PDF" }).click();
            const download = await downloadPromise;
            expect(download.suggestedFilename()).toMatch(/^.+\.pdf$/);
            await download.cancel();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ developerResourcesApiPage }) => {
                await expect(developerResourcesApiPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ developerResourcesApiPage }) => {
                await expect(developerResourcesApiPage.footer).not.toBeInViewport();
                await scrollToFooter(developerResourcesApiPage.page);
                await expect(developerResourcesApiPage.footer).toBeInViewport();
                await expect(developerResourcesApiPage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(developerResourcesApiPage.page);
                await expect(developerResourcesApiPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
