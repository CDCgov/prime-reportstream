import { expect, test } from "@playwright/test";

import { ExternalLinks } from "../helpers/external-links";
import { Utils } from "../helpers/utils";
import { GettingStarted } from "../pages/getting-started";
import { Header } from "../pages/header";
import { ManagingYourConnection } from "../pages/managing-your-connection";
import { OurNetwork } from "../pages/our-network";
import { Security } from "../pages/security";
import { Support } from "../pages/support";

test.describe("Homepage", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/");
    });

    test("has correct title", async ({ page }) => {
        await expect(page).toHaveTitle(
            /CDC's free, interoperable data transfer platform - ReportStream/,
        );
    });

    test("has About link and dropdown menu items", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnAbout();

        expect(true).toBe(true);
    });

    test("has Getting Started link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnGettingStarted();

        expect(true).toBe(true);
    });

    test("has Developers link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnDevelopers();

        expect(true).toBe(true);
    });

    test("has Your Connection link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnYourConnection();

        expect(true).toBe(true);
    });

    test("has Support link", async ({ page }) => {
        const header = new Header(page);
        await header.clickOnSupport();

        expect(true).toBe(true);
    });

    test('opens the "Connect with ReportStream" tab within header', async ({
        page,
    }) => {
        const externalLinks = new ExternalLinks(page);
        await externalLinks.clickOnConnect("header", "Connect with us");

        expect(true).toBe(true);
    });

    test('opens the "Connect with ReportStream" tab within footer', async ({
        page,
    }) => {
        const externalLinks = new ExternalLinks(page);
        await externalLinks.clickOnConnect("footer", "Connect now");

        expect(true).toBe(true);
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

        expect(true).toBe(true);
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

        expect(true).toBe(true);
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

        expect(true).toBe(true);
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

        expect(true).toBe(true);
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

        expect(true).toBe(true);
    });

    test("has clickable Where were live map", async ({ page }) => {
        const ourNetwork = new OurNetwork(page);
        const header = new Header(page);
        // Trigger map click and go to our network page
        await ourNetwork.clickOnLiveMap();
        // Go back to the homepage
        await header.clickOnHome();

        expect(true).toBe(true);
    });

    test("explicit scroll to footer and then scroll to top", async ({
        page,
    }) => {
        const utils = new Utils(page);
        await utils.scrollToFooter();
        await utils.scrollToTop();

        expect(true).toBe(true);
    });
});
