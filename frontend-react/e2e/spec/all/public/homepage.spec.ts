import { expect, test } from "@playwright/test";

import { scrollToFooter, scrollToTop } from "../../../helpers/utils";
import * as ourNetwork from "../../../pages/public/about/our-network";
import * as security from "../../../pages/public/about/security";
import * as header from "../../../pages/public/header";
import * as homepage from "../../../pages/public/homepage";
import * as managingYourConnection from "../../../pages/public/managing-your-connection/managing-your-connection";

test.describe(
    "Homepage",
    {
        tag: "@smoke",
    },
    () => {
        test.beforeEach(async ({ page }) => {
            await homepage.goto(page);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/ReportStream - CDC's free, interoperable data transfer platform/);
        });

        test("opens the Security page on 'security of your data' click", async ({ page }) => {
            await page.getByRole("link", { name: "security of your data" }).click();
            await security.onLoad(page);
            // Go back to the homepage
            await header.clickOnHome(page);

            expect(true).toBe(true);
        });

        test("opens the managing-your-connection page on 'our tools' click", async ({ page }) => {
            await page.getByRole("link", { name: "our tools" }).click();
            await managingYourConnection.onLoad(page);
            // Go back to the homepage
            await header.clickOnHome(page);

            expect(true).toBe(true);
        });

        test("opens Our Network page on 'See our full network' click", async ({ page }) => {
            await page.getByRole("link", { name: "See our full network" }).click();
            await ourNetwork.onLoad(page);
            // Go back to the homepage
            await header.clickOnHome(page);

            expect(true).toBe(true);
        });

        test("is clickable Where were live map", async ({ page }) => {
            // Trigger map click and go to our network page
            await ourNetwork.clickOnLiveMap(page);
            // Go back to the homepage
            await header.clickOnHome(page);

            expect(true).toBe(true);
        });

        test("explicit scroll to footer and then scroll to top", async ({ page }) => {
            await expect(page.locator("footer")).not.toBeInViewport();
            await scrollToFooter(page);
            await expect(page.locator("footer")).toBeInViewport();
            await expect(page.getByTestId("govBanner")).not.toBeInViewport();
            await scrollToTop(page);
            await expect(page.getByTestId("govBanner")).toBeInViewport();
        });
    },
);
