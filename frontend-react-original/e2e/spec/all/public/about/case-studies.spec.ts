import { aboutSideNav } from "../../../../helpers/internal-links";
import { AboutCaseStudiesPage } from "../../../../pages/public/about/case-studies";
import { test as baseTest } from "../../../../test";

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
        test("has side nav", async ({ aboutCaseStudiesPage }) => {
            await aboutCaseStudiesPage.testSidenav(aboutSideNav);
        });

        test("has correct title + heading", async ({ aboutCaseStudiesPage }) => {
            await aboutCaseStudiesPage.testHeader();
        });

        test("footer", async ({ aboutCaseStudiesPage }) => {
            await aboutCaseStudiesPage.testFooter();
        });
    },
);
