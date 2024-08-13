import { expect, test } from "@playwright/test";

import * as ourNetwork from "../../../../pages/public/about/our-network";
import * as sideNav from "../../../../pages/public/about-side-navigation";
test.describe(
    "Our network page",
    {
        tag: "@smoke",
    },
    () => {
        test.beforeEach(async ({ page }) => {
            await ourNetwork.goto(page);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Our network/);
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
    },
);
