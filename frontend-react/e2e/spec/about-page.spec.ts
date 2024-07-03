import { scrollToFooter, scrollToTop } from "../helpers/utils";
import { AboutPage } from "../pages/about";
import { test as baseTest, expect } from "../test";

const URL_ABOUT = "/about";

export interface AboutPageFixtures {
    aboutPage: AboutPage;
}

const test = baseTest.extend<AboutPageFixtures>({
    aboutPage: async (
        {
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        },
        use,
    ) => {
        const page = new AboutPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
        });
        await page.goto();
        await use(page);
    },
});

test.describe("About page", () => {
    test("nav contains the 'About' dropdown with 'About Reportstream' option", async ({
        aboutPage,
    }) => {
        const navItems = aboutPage.page.locator(".usa-nav  li");
        await expect(navItems).toContainText(["About"]);

        await aboutPage.page
            .getByTestId("header")
            .getByTestId("navDropDownButton")
            .getByText("About")
            .click();

        await aboutPage.page
            .getByTestId("header")
            .getByRole("link", { name: "About ReportStream" })
            .click();
        await expect(aboutPage.page).toHaveURL(URL_ABOUT);
    });

    test("has correct title", async ({ aboutPage }) => {
        await expect(aboutPage.page).toHaveTitle(/About/);
    });

    test.describe("In this section", () => {
        test("has 'Our network' link", async ({ aboutPage }) => {
            await aboutPage.page
                .getByRole("link", { name: /Our network/ })
                .click();
            await expect(aboutPage.page).toHaveURL(/.*about\/our-network/);
        });

        test("has 'Product roadmap' link", async ({ aboutPage }) => {
            await aboutPage.page
                .getByRole("link", { name: /Product roadmap/ })
                .click();
            await expect(aboutPage.page).toHaveURL(/.*about\/roadmap/);
        });

        test("has 'News' link", async ({ aboutPage }) => {
            await aboutPage.page.getByRole("link", { name: /News/ }).click();
            await expect(aboutPage.page).toHaveURL(/.*about\/news/);
        });

        test("has 'Case studies' link", async ({ aboutPage }) => {
            await aboutPage.page
                .getByRole("link", { name: /Case studies/ })
                .click();
            await expect(aboutPage.page).toHaveURL(/.*about\/case-studies/);
        });

        test("has 'Security' link", async ({ aboutPage }) => {
            await aboutPage.page
                .getByRole("link", { name: /Security/ })
                .click();
            await expect(aboutPage.page).toHaveURL(/.*about\/security/);
        });

        test("has 'Release notes' link", async ({ aboutPage }) => {
            await aboutPage.page
                .locator("div")
                .filter({ hasText: /^Release notes$/ })
                .getByRole("link")
                .click();
            await expect(aboutPage.page).toHaveURL(/.*about\/release-notes/);
        });
    });

    test.describe("Values section", () => {
        test("Accordion sections expand", async ({ aboutPage }) => {
            const accordionCol = [
                "We meet our partners where they are.",
                "We are free to use.",
                "We put better faster data into the hands of public health entities.",
                "We reduce disparities in healthcare monitoring and surveillance.",
            ];

            for (let i = 0; i < accordionCol.length; i++) {
                const accordionItem = `accordionItem_${i + 1}--content`;
                await expect(
                    aboutPage.page.getByTestId(accordionItem),
                ).toBeHidden();

                await aboutPage.page
                    .getByRole("button", {
                        name: accordionCol[i],
                    })
                    .click();

                await expect(
                    aboutPage.page.getByTestId(accordionItem),
                ).toBeVisible();

                await aboutPage.page
                    .getByRole("button", {
                        name: accordionCol[i],
                    })
                    .click();

                await expect(
                    aboutPage.page.getByTestId(accordionItem),
                ).toBeHidden();
            }
        });
    });

    test.describe("Recommended Resources", () => {
        test("Card navigation", async ({ aboutPage }) => {
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
                await aboutPage.page
                    .getByTestId("CardGroup")
                    .getByRole("link", { name: cardLink.name })
                    .click();

                if (cardLink.url.length) {
                    await expect(aboutPage.page).toHaveURL(cardLink.url);
                }

                await aboutPage.page.goto(URL_ABOUT, {
                    waitUntil: "domcontentloaded",
                });
            }
        });
    });

    test.describe("Footer", () => {
        test("has footer", async ({ aboutPage }) => {
            await expect(aboutPage.footer).toBeAttached();
        });

        test("explicit scroll to footer and then scroll to top", async ({
            aboutPage,
        }) => {
            await expect(aboutPage.footer).not.toBeInViewport();
            await scrollToFooter(aboutPage.page);
            await expect(aboutPage.footer).toBeInViewport();
            await expect(
                aboutPage.page.getByTestId("govBanner"),
            ).not.toBeInViewport();
            await scrollToTop(aboutPage.page);
            await expect(
                aboutPage.page.getByTestId("govBanner"),
            ).toBeInViewport();
        });
    });
});
