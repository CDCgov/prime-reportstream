import { ReferHealthcarePage } from "../../../../pages/public/managing-your-connection/refer-healthcare";
import { test as baseTest, expect } from "../../../../test";

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
        test.describe("Header", () => {
            test("has correct title + heading", async ({ referHealthcarePage }) => {
                await referHealthcarePage.testHeader();
            });
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
            test("has footer and explicit scroll to footer and scroll to top", async ({ referHealthcarePage }) => {
                await referHealthcarePage.testFooter();
            });
        });
    },
);
