import { ManagingYourConnectionPage } from "../../../../pages/public/managing-your-connection/managing-your-connection";
import { test as baseTest, expect } from "../../../../test";

const cards = [
    {
        name: "For healthcare organizations",
        links: ["Manage your public key", "View your submission history", "Login", "contact us"],
    },
    {
        name: "For public health agencies",
        links: ["Refer healthcare organizations", "View your dashboard", "Login", "contact us"],
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
        test.describe("Header", () => {
            test("has correct title + heading", async ({ managingYourConnectionPage }) => {
                await managingYourConnectionPage.testHeader();
            });
        });

        test.describe("Quick links", () => {
            for (const card of cards) {
                test(`should have ${card.name} links`, async ({ managingYourConnectionPage }) => {
                    const cardHeader = managingYourConnectionPage.page.locator(".usa-card__header", {
                        hasText: card.name,
                    });

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
            test("has footer and explicit scroll to footer and scroll to top", async ({
                managingYourConnectionPage,
            }) => {
                await managingYourConnectionPage.testFooter();
            });
        });
    },
);
