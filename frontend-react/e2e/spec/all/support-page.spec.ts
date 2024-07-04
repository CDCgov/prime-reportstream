import { expect, test } from "@playwright/test";
import fs from "fs";
import path from "path";
import * as support from "../../pages/support";

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

test.describe("Support page", () => {
    test.beforeEach(async ({ page }) => {
        await support.goto(page);
    });

    test("Should have the correct page header", async ({ page }) => {
        const header = page;

        await await expect(header).toHaveText("h1", "Support");
    });

    test("Should have a way of contacting support", async ({ page }) => {
        // Importing the actual URL of our contact form to match
        // what we use in production and only need to make updates
        // in a single place
        const jsonPath = path.resolve("./src/content/site.json");
        const site = JSON.parse(await fs.promises.readFile(jsonPath, "utf8"));
        const contactLink = page
            .locator(`a[href="${site.forms.contactUs.url}"]`)
            .first();

        await contactLink.scrollIntoViewIfNeeded();
        await expect(contactLink).toBeVisible();
    });

    for (const card of cards) {
        // eslint-disable-next-line playwright/expect-expect
        test(`should have ${card.name} link`, async ({ page }) => {
            const cardHeader = page.locator(".usa-card__header", {
                hasText: card.name,
            });

            await expect(cardHeader).toBeVisible();

            const cardContainer = cardHeader.locator("..");
            const viewAllLink = cardContainer.locator("a").last();

            await viewAllLink.click();
            await expect(page.locator(`#${card.anchorID}`)).toBeVisible();
        });
    }
});
