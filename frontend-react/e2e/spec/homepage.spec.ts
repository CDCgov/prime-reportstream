import { expect, test } from "@playwright/test";

import * as externalLinks from "../helpers/external-links";
import { CONNECT_URL } from "../helpers/external-links";
import { scrollToFooter, scrollToTop } from "../helpers/utils";
import * as gettingStarted from "../pages/getting-started";
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
        await homepage.onLoad(page);
    });

    test("has About link and dropdown menu items", async ({ page }) => {
        await header.clickOnAbout(page);

        expect(true).toBe(true);
    });

    test("has Getting Started link", async ({ page }) => {
        await header.clickOnGettingStarted(page);

        expect(true).toBe(true);
    });

    test("has Developers link", async ({ page }) => {
        await header.clickOnDevelopers(page);

        expect(true).toBe(true);
    });

    test("has Your Connection link", async ({ page }) => {
        await header.clickOnYourConnection(page);

        expect(true).toBe(true);
    });

    test("has Support link", async ({ page }) => {
        await header.clickOnSupport(page);

        expect(true).toBe(true);
    });

    test('opens the "Connect with ReportStream" tab within header', async ({
        page,
    }) => {
        await externalLinks.clickOnExternalLink(
            "header",
            "Connect with us",
            page,
            CONNECT_URL,
        );

        expect(true).toBe(true);
    });

    test('opens the "Connect with ReportStream" tab within footer', async ({
        page,
    }) => {
        await externalLinks.clickOnExternalLink(
            "footer",
            "Connect now",
            page,
            CONNECT_URL,
        );

        expect(true).toBe(true);
    });

    test("opens the Getting started page on 'how to set up transfer' click", async ({
        page,
    }) => {
        await page
            .getByRole("link", { name: "how to set up transfer" })
            .click();
        await gettingStarted.onLoad(page);
        // Go back to the homepage
        await header.clickOnHome(page);

        expect(true).toBe(true);
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
        await scrollToFooter(page);
        await scrollToTop(page);
    });
});
