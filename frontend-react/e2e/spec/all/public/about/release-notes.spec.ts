import { scrollToFooter, scrollToTop } from "../../../../helpers/utils";
import { AboutReleaseNotesPage } from "../../../../pages/about/release-notes";
import { test as baseTest, expect } from "../../../../test";

export interface Fixtures {
    aboutReleaseNotesPage: AboutReleaseNotesPage;
}

const test = baseTest.extend<Fixtures>({
    aboutReleaseNotesPage: async (
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
        const page = new AboutReleaseNotesPage({
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
    "About / Release Notes page",
    {
        tag: "@smoke",
    },
    () => {
        test("has correct title", async ({ aboutReleaseNotesPage }) => {
            await expect(aboutReleaseNotesPage.page).toHaveTitle(aboutReleaseNotesPage.title);
            await expect(aboutReleaseNotesPage.heading).toBeVisible();
        });

        test("has side nav", async ({ aboutReleaseNotesPage }) => {
            await expect(
                aboutReleaseNotesPage.page.getByRole("navigation", { name: "side-navigation " }),
            ).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ aboutReleaseNotesPage }) => {
                await expect(aboutReleaseNotesPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ aboutReleaseNotesPage }) => {
                await expect(aboutReleaseNotesPage.footer).not.toBeInViewport();
                await scrollToFooter(aboutReleaseNotesPage.page);
                await expect(aboutReleaseNotesPage.footer).toBeInViewport();
                await expect(aboutReleaseNotesPage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(aboutReleaseNotesPage.page);
                await expect(aboutReleaseNotesPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
