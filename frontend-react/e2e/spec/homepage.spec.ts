import { expect, test } from "@playwright/test";

import { scrollToFooter, scrollToTop } from "../helpers/utils";
import * as header from "../pages/header";
import * as homepage from "../pages/homepage";
import * as managingYourConnection from "../pages/managing-your-connection";
import * as ourNetwork from "../pages/our-network";
import * as security from "../pages/security";
import * as support from "../pages/support";

test.describe("Homepage", () => {
    test.beforeEach(async ({ page }) => {
        await homepage.goto(page);
    });

    test("has correct title", async ({ page }) => {
        await expect(page).toHaveTitle(
            /ReportStream - CDC's free, interoperable data transfer platform/,
        );
    });

    test("opens the Security page on 'security of your data' click", async ({
        page,
    }) => {
        await page.getByRole("link", { name: "security of your data" }).click();
        await security.onLoad(page);
        // Go back to the homepage
        await header.clickOnHome(page);

        expect(true).toBe(true);
    });

    test("opens the Support page on 'expert support team' click", async ({
        page,
    }) => {
        await page.getByRole("link", { name: "expert support team" }).click();
        await support.onLoad(page);
        // Go back to the homepage
        await header.clickOnHome(page);

        expect(true).toBe(true);
    });

    test("opens the managing-your-connection page on 'our tools' click", async ({
        page,
    }) => {
        await page.getByRole("link", { name: "our tools" }).click();
        await managingYourConnection.onLoad(page);
        // Go back to the homepage
        await header.clickOnHome(page);

        expect(true).toBe(true);
    });

    test("opens Our Network page on 'See our full network' click", async ({
        page,
    }) => {
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

    test("explicit scroll to footer and then scroll to top", async ({
        page,
    }) => {
        await expect(page.locator("footer")).not.toBeInViewport();
        await scrollToFooter(page);
        await expect(page.locator("footer")).toBeInViewport();
        await expect(page.getByTestId("govBanner")).not.toBeInViewport();
        await scrollToTop(page);
        await expect(page.getByTestId("govBanner")).toBeInViewport();
    });
});
