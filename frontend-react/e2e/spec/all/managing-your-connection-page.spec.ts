import { scrollToFooter, scrollToTop } from "../../helpers/utils";
import { ManagingYourConnectionPage } from "../../pages/managing-your-connection";
import { test as baseTest, expect } from "../../test";

const cards = [
    {
        name: "For healthcare organizations",
        links: [
            "Manage your public key",
            "View your submission history",
            "Login",
            "contact us",
        ],
    },
    {
        name: "For public health agencies",
        links: [
            "Refer healthcare organizations",
            "View your dashboard",
            "Login",
            "contact us",
        ],
    },
];

export interface ManagingYourConnectionPageFixtures {
    managingYourConnectionPage: ManagingYourConnectionPage;
}

const test = baseTest.extend<ManagingYourConnectionPageFixtures>({
    managingYourConnectionPage: async (
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
        const page = new ManagingYourConnectionPage({
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

test.describe(
    "Managing Your Connection page",
    {
        tag: "@smoke",
    },
    () => {
        test("has correct title", async ({ managingYourConnectionPage }) => {
            await expect(managingYourConnectionPage.page).toHaveTitle(
                managingYourConnectionPage.title,
            );
            await expect(managingYourConnectionPage.heading).toBeVisible();
        });

        test.describe("Quick links", () => {
            for (const card of cards) {
                // eslint-disable-next-line playwright/expect-expect
                test(`should have ${card.name} links`, async ({
                    managingYourConnectionPage,
                }) => {
                    const cardHeader = managingYourConnectionPage.page.locator(
                        ".usa-card__header",
                        {
                            hasText: card.name,
                        },
                    );

                    await expect(cardHeader).toBeVisible();

                    const cardContainer = cardHeader.locator("..");

                    for (const link of card.links) {
                        await expect(
                            cardContainer.getByRole("link", {
                                name: `${link}`,
                            }),
                        ).toBeVisible();
                    }
                });
            }
        });

        test.describe("Footer", () => {
            test("has footer", async ({ managingYourConnectionPage }) => {
                await expect(managingYourConnectionPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({
                managingYourConnectionPage,
            }) => {
                await expect(
                    managingYourConnectionPage.footer,
                ).not.toBeInViewport();
                await scrollToFooter(managingYourConnectionPage.page);
                await expect(
                    managingYourConnectionPage.footer,
                ).toBeInViewport();
                await expect(
                    managingYourConnectionPage.page.getByTestId("govBanner"),
                ).not.toBeInViewport();
                await scrollToTop(managingYourConnectionPage.page);
                await expect(
                    managingYourConnectionPage.page.getByTestId("govBanner"),
                ).toBeInViewport();
            });
        });
    },
);
