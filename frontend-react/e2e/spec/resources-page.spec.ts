import { expect, test } from "@playwright/test";
import * as resources from "../pages/resources";

// eslint-disable-next-line playwright/no-skipped-test
test.describe.skip("Developer Resources page", () => {
    test.beforeEach(async ({ page }) => {
        await resources.goto(page);
    });

    test("should have correct title", async ({ page }) => {
        await expect(page).toHaveURL(/developer-resources/);
        await expect(page).toHaveTitle(/Developer resources/);
    });

    // TODO: Fix
    test.describe("Card navigation", () => {
        const cardLinks = [
            {
                name: "Security practices",
                url: "/resources/security-practices",
            },
            {
                name: "System and settings",
                url: "/resources/system-and-settings",
            },
            {
                name: "ReportStream API",
                url: "/resources/api",
            },
            {
                name: "Guide to submitting data to ReportStream",
                url: "/resources/getting-started-submitting-data",
            },
            {
                name: "ReportStream File Validator",
                url: "/file-handler/validate",
            },
            {
                name: "Account Registration Guide",
                url: "/resources/account-registration-guide",
            },
            {
                name: "ELR Onboarding Checklist",
                url: "/resources/elr-checklist",
            },
            {
                name: "Guide to receiving ReportStream data",
                url: "/resources/getting-started-public-health-departments",
            },
            {
                name: "Manual data download guide",
                url: "/resources/data-download-guide",
            },
            {
                name: "ReportStream Referral Guide",
                url: "/resources/referral-guide",
            },
        ];

        for (const cardLink of cardLinks) {
            test(`should have ${cardLink.name} link`, async ({ page }) => {
                await page.getByRole("link", { name: cardLink.name }).click();

                await expect(page).toHaveURL(cardLink.url);
            });
        }

        test("should redirect unauthenticated users to login page on managing public key", async ({
            page,
        }) => {
            await page
                .getByRole("link", {
                    name: "Manage your public key",
                })
                .click();

            await expect(page).toHaveURL("/login");
        });
    });
});
