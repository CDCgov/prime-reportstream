import { scrollToFooter, scrollToTop } from "../../../helpers/utils";
import { AboutRoadmapPage } from "../../../pages/about/roadmap";
import { test as baseTest, expect } from "../../../test";

export interface Fixtures {
    aboutRoadmapPage: AboutRoadmapPage;
}

const test = baseTest.extend<Fixtures>({
    aboutRoadmapPage: async (
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
        const page = new AboutRoadmapPage({
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
        test("has correct title", async ({ aboutRoadmapPage }) => {
            await expect(aboutRoadmapPage.page).toHaveTitle(aboutRoadmapPage.title);
            await expect(aboutRoadmapPage.heading).toBeVisible();
        });

        test("has side nav", async ({ aboutRoadmapPage }) => {
            await expect(aboutRoadmapPage.page.getByRole("navigation", { name: "side-navigation " })).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ aboutRoadmapPage }) => {
                await expect(aboutRoadmapPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ aboutRoadmapPage }) => {
                await expect(aboutRoadmapPage.footer).not.toBeInViewport();
                await scrollToFooter(aboutRoadmapPage.page);
                await expect(aboutRoadmapPage.footer).toBeInViewport();
                await expect(aboutRoadmapPage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(aboutRoadmapPage.page);
                await expect(aboutRoadmapPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
