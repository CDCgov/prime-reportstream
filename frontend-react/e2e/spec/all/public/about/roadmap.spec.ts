import { expect, test } from "@playwright/test";

import * as internalLinks from "../../../../helpers/internal-links";
import * as roadmap from "../../../../pages/public/about/roadmap";
import { URL_ROADMAP } from "../../../../pages/public/about/roadmap";
import * as sideNav from "../../../../pages/public/about-side-navigation";

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

        test.describe("Additional resources Links", () => {
            test("has News", async ({ page }) => {
                await internalLinks.clickOnInternalLink("div", "CardGroup", "News", page);
                await expect(page).toHaveURL(/.*about\/news/);
            });

            test("has Release notes", async ({ page }) => {
                await internalLinks.clickOnInternalLink("div", "CardGroup", "Release notes", page);
                await expect(page).toHaveURL(/.*about\/release-notes/);
            });

            test("has Developer resources", async ({ page }) => {
                await internalLinks.clickOnInternalLink("div", "CardGroup", "Developer resources", page);
                await expect(page).toHaveURL(/.*developer-resources/);
            });
        });
    },
);
