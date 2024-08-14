import { scrollToFooter, scrollToTop } from "../../helpers/utils";
import { ReferHealthcarePage } from "../../pages/refer-healthcare";
import { test as baseTest, expect } from "../../test";

export interface ReferHealthcarePageFixtures {
    referHealthcarePage: ReferHealthcarePage;
}

const test = baseTest.extend<ReferHealthcarePageFixtures>({
    referHealthcarePage: async (
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
        const page = new ReferHealthcarePage({
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
    "Managing Your Connection page",
    {
        tag: "@smoke",
    },
    () => {
        test("has correct title", async ({ referHealthcarePage }) => {
            await expect(referHealthcarePage.page).toHaveTitle(referHealthcarePage.title);
            await expect(referHealthcarePage.heading).toBeVisible();
        });

        test("has correct sidenav items", async ({ referHealthcarePage }) => {
            await expect(
                referHealthcarePage.page
                    .getByTestId("sidenav")
                    .getByRole("link", { name: /Refer healthcare organizations/ }),
            ).toBeVisible();
            await expect(
                referHealthcarePage.page.getByTestId("sidenav").getByRole("link", { name: /Manage public key/ }),
            ).toBeVisible();
        });

        test("has link to referral email template", async ({ referHealthcarePage }) => {
            await expect(
                referHealthcarePage.page.getByRole("link", { name: /referral email template/i }),
            ).toBeVisible();

            await referHealthcarePage.page.getByRole("link", { name: /referral email template/i }).click();

            await expect(referHealthcarePage.page.locator("#email-template")).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ referHealthcarePage }) => {
                await expect(referHealthcarePage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ referHealthcarePage }) => {
                await expect(referHealthcarePage.footer).not.toBeInViewport();
                await scrollToFooter(referHealthcarePage.page);
                await expect(referHealthcarePage.footer).toBeInViewport();
                await expect(referHealthcarePage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(referHealthcarePage.page);
                await expect(referHealthcarePage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
