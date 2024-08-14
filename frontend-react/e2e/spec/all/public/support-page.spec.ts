import site from "../../../../src/content/site.json" assert { type: "json" };
import { scrollToFooter, scrollToTop, testFooter } from "../../../helpers/utils";
import { SupportPage } from "../../../pages/public/support.js";
import { test as baseTest, expect } from "../../../test";

const cards = [
    {
        name: "Welcome to ReportStream",
        anchorID: "welcome-to-reportstream-1",
    },
    {
        name: "Getting started",
        anchorID: "getting-started-1",
    },
    {
        name: "Using ReportStream",
        anchorID: "using-reportstream-1",
    },
];

export interface SupportPageFixtures {
    supportPage: SupportPage;
}

const test = baseTest.extend<SupportPageFixtures>({
    supportPage: async (
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
        const page = new SupportPage({
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

test.describe("Support page", () => {
    test("has correct title", async ({ supportPage }) => {
        await expect(supportPage.page).toHaveTitle(supportPage.title);
        await expect(supportPage.heading).toBeVisible();
    });

    test("Should have a way of contacting support", async ({ supportPage }) => {
        const contactLink = supportPage.page.locator(`a[href="${site.forms.contactUs.url}"]`).first();

        await contactLink.scrollIntoViewIfNeeded();
        await expect(contactLink).toBeVisible();
    });

    for (const card of cards) {
        test(`should have ${card.name} link`, async ({ supportPage }) => {
            const cardHeader = supportPage.page.locator(".usa-card__header", {
                hasText: card.name,
            });

            await expect(cardHeader).toBeVisible();

            const cardContainer = cardHeader.locator("..");
            const viewAllLink = cardContainer.locator("a").last();

            await viewAllLink.click();
            await expect(supportPage.page.locator(`#${card.anchorID}`)).toBeVisible();
        });
    }

    test.describe("Footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({ supportPage }) => {
            await testFooter(supportPage);
        });
    });
});
