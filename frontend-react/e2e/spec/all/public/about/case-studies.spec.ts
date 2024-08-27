import { AboutCaseStudiesPage } from "../../../../pages/public/about/case-studies";
import { test as baseTest, expect } from "../../../../test";

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

        test("has correct title + heading", async ({ aboutCaseStudiesPage }) => {
            await aboutCaseStudiesPage.testHeader();
        });

        test("footer", async ({ aboutCaseStudiesPage }) => {
            await aboutCaseStudiesPage.testFooter();
        });
    },
);
