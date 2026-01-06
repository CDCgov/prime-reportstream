import site from "../../../../src/content/site.json" assert { type: "json" };
import { HomePage } from "../../../pages/public/homepage";
import { test as baseTest, expect } from "../../../test";

export interface HomePageFixtures {
    homePage: HomePage;
}

const test = baseTest.extend<HomePageFixtures>({
    homePage: async (
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
        const page = new HomePage({
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
    "Homepage",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("Header", () => {
            test("has correct title + heading", async ({ homePage }) => {
                await homePage.testHeader();
            });
        });

        test.describe("CTA", () => {
            test("has 'Contact us' button", async ({ homePage }) => {
                const heroLocator = homePage.page.locator('[class*="hero-wrapper"]');
                const ctaURL = site.forms.connectWithRS.url;
                const ctaLink = heroLocator.locator(`a[href="${ctaURL}"]`).first();

                await expect(ctaLink).toBeVisible();
            });
        });

        test.describe("Footer", () => {
            test("has footer and explicit scroll to footer and scroll to top", async ({ homePage }) => {
                await homePage.testFooter();
            });
        });
    },
);
