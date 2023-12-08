import { test, expect } from "@playwright/test";

import { Header } from "../pages/header";
import { OurNetwork } from "../pages/our-network";
import { ExternalLinks } from "../helpers/external-links";
import { Utils } from "../helpers/utils";

test.describe("Homepage", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/");
    });

    test("Has correct title", async ({ page }) => {
        await expect(page).toHaveTitle(/CDC Prime ReportStream/);
    });

    test("has About link and dropdown menu items", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnAbout();
    });

    test("has Getting Started link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnGettingStarted();
    });

    test("has Developers link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnDevelopers();
    });

    test("has Your Connection link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnYourConnection();
    });

    test("has Support link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnSupport();
    });

    test('opens the "Connect with ReportStream" tab within header', async ({
        page,
    }) => {
        const externalLinks = new ExternalLinks(page);
        await externalLinks.clickOnConnect("header", "Connect with us");
    });

    test('opens the "Connect with ReportStream" tab within footer', async ({
        page,
    }) => {
        const externalLinks = new ExternalLinks(page);
        await externalLinks.clickOnConnect("footer", "Connect now");
    });

    test("has clickable Where were live map", async ({ page }) => {
        const ourNetwork = new OurNetwork(page);
        const header = new Header(page);
        // Trigger map click and go to our network page
        await ourNetwork.clickOnLiveMap();
        // Go back to the homepage
        await header.clickOnHome();
    });

    test("explicit scroll to footer and then scroll to top", async ({
        page,
    }) => {
        const utils = new Utils(page);
        await utils.scrollToFooter();
        await utils.scrollToTop();
    });
});
