import { test, expect } from "@playwright/test";

import { Header } from "../pages/header";
import { OurNetwork } from "../pages/our-network";
import { ExternalLinks } from "../helpers/external-links";
import { Utils } from "../helpers/utils";
import { Security } from "../pages/security";
import { GettingStarted } from "../pages/getting-started";
import { Support } from "../pages/support";
import { ManagingYourConnection } from "../pages/managing-your-connection";

test.describe("Homepage", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/");
    });

    test("Has correct title", async ({ page }) => {
        await expect(page).toHaveTitle(
            /CDC's free, interoperable data transfer platform - ReportStream/,
        );
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

    test("opens the Getting started page on 'how to set up transfer' click", async ({
        page,
    }) => {
        const header = new Header(page);
        const gettingStarted = new GettingStarted(page);

        await page
            .getByRole("link", { name: "how to set up transfer" })
            .click();
        await gettingStarted.onLoad();
        // Go back to the homepage
        await header.clickOnHome();
    });

    test("opens the Security page on 'security of your data' click", async ({
        page,
    }) => {
        const header = new Header(page);
        const security = new Security(page);

        await page.getByRole("link", { name: "security of your data" }).click();
        await security.onLoad();
        // Go back to the homepage
        await header.clickOnHome();
    });

    test("opens the Support page on 'expert support team' click", async ({
        page,
    }) => {
        const header = new Header(page);
        const support = new Support(page);

        await page.getByRole("link", { name: "expert support team" }).click();
        await support.onLoad();
        // Go back to the homepage
        await header.clickOnHome();
    });

    test("opens the managing-your-connection page on 'our tools' click", async ({
        page,
    }) => {
        const header = new Header(page);
        const managingYourConnection = new ManagingYourConnection(page);

        await page.getByRole("link", { name: "our tools" }).click();
        await managingYourConnection.onLoad();
        // Go back to the homepage
        await header.clickOnHome();
    });

    test("opens Our Network page on 'See our full network' click", async ({
        page,
    }) => {
        const ourNetwork = new OurNetwork(page);
        const header = new Header(page);

        await page.getByRole("link", { name: "See our full network" }).click();
        await ourNetwork.onLoad();
        // Go back to the homepage
        await header.clickOnHome();
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
