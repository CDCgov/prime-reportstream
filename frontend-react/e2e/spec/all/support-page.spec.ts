import fs from "fs";
import path from "path";
import { SupportPage } from "../../pages/support.js";
import { test as baseTest, expect } from "../../test";

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
    test("Should have a way of contacting support", async ({ supportPage }) => {
        // Importing the actual URL of our contact form to match
        // what we use in production and only need to make updates
        // in a single place
        const jsonPath = path.resolve("./src/content/site.json");
        const site = JSON.parse(await fs.promises.readFile(jsonPath, "utf8"));
        const contactLink = supportPage.page
            .locator(`a[href="${site.forms.contactUs.url}"]`)
            .first();

        await contactLink.scrollIntoViewIfNeeded();
        await expect(contactLink).toBeVisible();
    });

    for (const card of cards) {
        // eslint-disable-next-line playwright/expect-expect
        test(`should have ${card.name} link`, async ({ supportPage }) => {
            const cardHeader = supportPage.page.locator(".usa-card__header", {
                hasText: card.name,
            });

            await expect(cardHeader).toBeVisible();

            const cardContainer = cardHeader.locator("..");
            const viewAllLink = cardContainer.locator("a").last();

            await viewAllLink.click();
            await expect(
                supportPage.page.locator(`#${card.anchorID}`),
            ).toBeVisible();
        });
    }
});
