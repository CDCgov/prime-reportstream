import { expect, test } from "@playwright/test";

import * as externalLinks from "../helpers/external-links";
import {
    MAKE_MY_TEST_COUNT,
    RADX_MARS,
    SIMPLEREPORT,
} from "../helpers/external-links";
import * as internalLinks from "../helpers/internal-links";
import { ELC, NIST } from "../helpers/internal-links";
import * as sideNav from "../pages/about-side-navigation";
import * as roadmap from "../pages/roadmap";
test.describe("Product roadmap page", () => {
    test.beforeEach(async ({ page }) => {
        await roadmap.goto(page);
    });

    test("has correct title", async ({ page }) => {
        await roadmap.onLoad(page);
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
            await linksCount.nth(0).click();
            await expect(page).toHaveURL(ELC);
        });

        test("has 'SimpleReport'", async ({ page }) => {
            await externalLinks.clickOnExternalLink(
                "article",
                "SimpleReport",
                page,
                SIMPLEREPORT,
            );
        });

        test("has 'RADx MARS'", async ({ page }) => {
            await externalLinks.clickOnExternalLink(
                "article",
                "RADx MARS",
                page,
                RADX_MARS,
            );
        });

        // TODO: figure out how to open .pdf docs in playwright
        // test("has 'NIST HL7 2.5.1'", async ({ page }) => {
        //     await page.getByRole("link", { name: "NIST HL7 2.5.1" }).click();
        //     await expect(page).toHaveURL(NIST);
        // });

        test("has 'MakeMyTestCount.org'", async ({ page }) => {
            await externalLinks.clickOnExternalLink(
                "article",
                "MakeMyTestCount.org",
                page,
                MAKE_MY_TEST_COUNT,
            );
        });
    });

    test.describe("Additional resources Links", () => {
        test("has News", async ({ page }) => {
            await internalLinks.clickOnInternalLink(
                "div",
                "CardGroup",
                "News",
                page,
                /.*about\/news/,
            );
        });

        test("has Release notes", async ({ page }) => {
            await internalLinks.clickOnInternalLink(
                "div",
                "CardGroup",
                "Release notes",
                page,
                /.*about\/release-notes/,
            );
        });

        test("has Developer resources", async ({ page }) => {
            await internalLinks.clickOnInternalLink(
                "div",
                "CardGroup",
                "Developer resources",
                page,
                /.*developer-resources/,
            );
        });
    });
});
