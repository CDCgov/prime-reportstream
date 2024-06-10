import { expect, test } from "@playwright/test";
import { scrollToFooter, scrollToTop } from "../helpers/utils";

const URL_ABOUT = "/about";
test.describe("About page", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto(URL_ABOUT, {
            waitUntil: "domcontentloaded",
        });
    });

    test("nav contains the 'About' dropdown with 'About Reportstream' option", async ({
        page,
    }) => {
        const navItems = page.locator(".usa-nav  li");
        await expect(navItems).toContainText(["About"]);

        await page
            .getByTestId("header")
            .getByTestId("navDropDownButton")
            .getByText("About")
            .click();

        await page
            .getByTestId("header")
            .getByRole("link", { name: "About ReportStream" })
            .click();
        await expect(page).toHaveURL(URL_ABOUT);
    });

    test("has correct title", async ({ page }) => {
        await expect(page).toHaveTitle(/About/);
    });

    test.describe("In this section", () => {
        test("has 'Our network' link", async ({ page }) => {
            await page.getByRole("link", { name: /Our network/ }).click();
            await expect(page).toHaveURL(/.*about\/our-network/);
        });

        test("has 'Product roadmap' link", async ({ page }) => {
            await page.getByRole("link", { name: /Product roadmap/ }).click();
            await expect(page).toHaveURL(/.*about\/roadmap/);
        });

        test("has 'News' link", async ({ page }) => {
            await page.getByRole("link", { name: /News/ }).click();
            await expect(page).toHaveURL(/.*about\/news/);
        });

        test("has 'Case studies' link", async ({ page }) => {
            await page.getByRole("link", { name: /Case studies/ }).click();
            await expect(page).toHaveURL(/.*about\/case-studies/);
        });

        test("has 'Security' link", async ({ page }) => {
            await page.getByRole("link", { name: /Security/ }).click();
            await expect(page).toHaveURL(/.*about\/security/);
        });

        test("has 'Release notes' link", async ({ page }) => {
            await page
                .locator("div")
                .filter({ hasText: /^Release notes$/ })
                .getByRole("link")
                .click();
            await expect(page).toHaveURL(/.*about\/release-notes/);
        });
    });

    test.describe("Values section", () => {
        test("Accordion sections expand", async ({ page }) => {
            const accordionCol = [
                "We meet our partners where they are.",
                "We are free to use.",
                "We put better faster data into the hands of public health entities.",
                "We reduce disparities in healthcare monitoring and surveillance.",
            ];

            for (let i = 0; i < accordionCol.length; i++) {
                const accordionItem = `accordionItem_${i + 1}--content`;
                await expect(page.getByTestId(accordionItem)).toBeHidden();

                await page
                    .getByRole("button", {
                        name: accordionCol[i],
                    })
                    .click();

                await expect(page.getByTestId(accordionItem)).toBeVisible();

                await page
                    .getByRole("button", {
                        name: accordionCol[i],
                    })
                    .click();

                await expect(page.getByTestId(accordionItem)).toBeHidden();
            }
        });
    });

    test.describe("Recommended Resources", () => {
        test("Card navigation", async ({ page }) => {
            const cardLinks = [
                {
                    name: "ReportStream overview PDF",
                    url: "",
                },
                {
                    name: "ReportStream API",
                    url: "/developer-resources/api",
                },
                {
                    name: "Release notes",
                    url: "/about/release-notes",
                },
            ];

            for (const cardLink of cardLinks) {
                await page
                    .getByTestId("CardGroup")
                    .getByRole("link", { name: cardLink.name })
                    .click();

                if (cardLink.url.length) {
                    await expect(page).toHaveURL(cardLink.url);
                }

                await page.goto(URL_ABOUT, {
                    waitUntil: "domcontentloaded",
                });
            }
        });
    });

    test.describe("Footer", () => {
        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
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
});
