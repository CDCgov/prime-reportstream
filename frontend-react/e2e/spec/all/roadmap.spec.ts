import { expect, test } from "@playwright/test";

import * as externalLinks from "../../helpers/external-links";
import {
    MAKE_MY_TEST_COUNT,
    RADX_MARS,
    SIMPLEREPORT,
} from "../../helpers/external-links";
import * as internalLinks from "../../helpers/internal-links";
import { ELC } from "../../helpers/internal-links";
import * as sideNav from "../../pages/about-side-navigation";
import * as roadmap from "../../pages/roadmap";
import { URL_ROADMAP } from "../../pages/roadmap";

test.describe(
    "Product roadmap page",
    {
        tag: "@smoke",
    },
    () => {
        test.beforeEach(async ({ page }) => {
            await roadmap.goto(page);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveURL(URL_ROADMAP);
            await expect(page).toHaveTitle(/Product roadmap/);
        });

        test.describe("Side navigation", () => {
            test("has Our network link", async ({ page }) => {
                await sideNav.clickNetwork(page);
                await expect(page).toHaveURL(/.*about\/our-network/);
            });

            test("has Product roadmap link", async ({ page }) => {
                await sideNav.clickRoadmap(page);
                await expect(page).toHaveURL(/.*about\/roadmap/);
            });

            test("has News link", async ({ page }) => {
                await sideNav.clickNews(page);
                await expect(page).toHaveURL(/.*about\/news/);
            });

            test("has Case studies link", async ({ page }) => {
                await sideNav.clickCaseStudies(page);
                await expect(page).toHaveURL(/.*about\/case-studies/);
            });

            test("has Security link", async ({ page }) => {
                await sideNav.clickSecurity(page);
                await expect(page).toHaveURL(/.*about\/security/);
            });

            test("has Release notes link", async ({ page }) => {
                await sideNav.clickReleaseNotes(page);
                await expect(page).toHaveURL(/.*about\/release-notes/);
            });
        });

        test.describe("Article Links", () => {
            test("has 'ELC-funded'", async ({ page }) => {
                const linksCount = page
                    .locator("article")
                    .getByRole("link", { name: "ELC-funded" });
                await expect(linksCount).toHaveCount(2);
                const newTabPromise = page.waitForEvent("popup");
                await linksCount.nth(1).click();
                const newTab = await newTabPromise;
                await newTab.waitForLoadState();
                await expect(newTab).toHaveURL(ELC);
            });

            test("has 'SimpleReport'", async ({ page }) => {
                const newTab = await externalLinks.clickOnExternalLink(
                    "article",
                    "SimpleReport",
                    page,
                );
                await expect(newTab).toHaveURL(SIMPLEREPORT);
            });

            test("has 'RADx MARS'", async ({ page }) => {
                const newTab = await externalLinks.clickOnExternalLink(
                    "article",
                    "RADx MARS",
                    page,
                );
                await expect(newTab).toHaveURL(RADX_MARS);
            });

            // TODO: figure out how to open .pdf docs in playwright
            test.skip("has 'NIST HL7 2.5.1'", async ({ page }) => {
                await page
                    .getByRole("link", { name: "NIST HL7 2.5.1" })
                    .click();
                await expect(page).toHaveURL(
                    "https://www.cdc.gov/vaccines/programs/iis/technical-guidance/downloads/hl7guide-1-5-2014-11.pdf",
                );
            });

            test("has 'MakeMyTestCount.org'", async ({ page }) => {
                const newTab = await externalLinks.clickOnExternalLink(
                    "article",
                    "MakeMyTestCount.org",
                    page,
                );
                await expect(newTab).toHaveURL(MAKE_MY_TEST_COUNT);
            });
        });

        test.describe("Additional resources Links", () => {
            test("has News", async ({ page }) => {
                await internalLinks.clickOnInternalLink(
                    "div",
                    "CardGroup",
                    "News",
                    page,
                );
                await expect(page).toHaveURL(/.*about\/news/);
            });

            test("has Release notes", async ({ page }) => {
                await internalLinks.clickOnInternalLink(
                    "div",
                    "CardGroup",
                    "Release notes",
                    page,
                );
                await expect(page).toHaveURL(/.*about\/release-notes/);
            });

            test("has Developer resources", async ({ page }) => {
                await internalLinks.clickOnInternalLink(
                    "div",
                    "CardGroup",
                    "Developer resources",
                    page,
                );
                await expect(page).toHaveURL(/.*developer-resources/);
            });
        });
    },
);
