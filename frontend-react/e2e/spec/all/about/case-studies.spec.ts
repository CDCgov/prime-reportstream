import { scrollToFooter, scrollToTop } from "../../../helpers/utils";
import { AboutCaseStudiesPage } from "../../../pages/about/case-studies";
import { test as baseTest, expect } from "../../../test";

export interface Fixtures {
    aboutCaseStudiesPage: AboutCaseStudiesPage;
}

const test = baseTest.extend<Fixtures>({
    aboutCaseStudiesPage: async (
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
        const page = new AboutCaseStudiesPage({
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
    "About / Case Studies page",
    {
        tag: "@smoke",
    },
    () => {
        test("has correct title", async ({ aboutCaseStudiesPage }) => {
            await expect(aboutCaseStudiesPage.page).toHaveTitle(aboutCaseStudiesPage.title);
            await expect(aboutCaseStudiesPage.heading).toBeVisible();
        });

        test("has side nav", async ({ aboutCaseStudiesPage }) => {
            await expect(aboutCaseStudiesPage.page.getByRole("navigation", { name: "side-navigation " })).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ aboutCaseStudiesPage }) => {
                await expect(aboutCaseStudiesPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ aboutCaseStudiesPage }) => {
                await expect(aboutCaseStudiesPage.footer).not.toBeInViewport();
                await scrollToFooter(aboutCaseStudiesPage.page);
                await expect(aboutCaseStudiesPage.footer).toBeInViewport();
                await expect(aboutCaseStudiesPage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(aboutCaseStudiesPage.page);
                await expect(aboutCaseStudiesPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
