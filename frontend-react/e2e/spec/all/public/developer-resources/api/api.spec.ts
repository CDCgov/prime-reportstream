import { DeveloperResourcesApiPage } from "../../../../../pages/public/developer-resources/api/api";
import { test as baseTest, expect } from "../../../../../test.js";

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
            await developerResourcesApiPage.testSidenav([]);
        });

        test("pdf file download works", async ({ developerResourcesApiPage }) => {
            const downloadPromise = developerResourcesApiPage.page.waitForEvent("download");
            await developerResourcesApiPage.page.getByRole("link", { name: "downloadable PDF" }).click();
            const download = await downloadPromise;
            expect(download.suggestedFilename()).toMatch(/^.+\.pdf$/);
            await download.cancel();
        });

        test("has correct title + heading", async ({ developerResourcesApiPage }) => {
            await developerResourcesApiPage.testHeader();
        });

        test("footer", async ({ developerResourcesApiPage }) => {
            await developerResourcesApiPage.testFooter();
        });
    },
);
