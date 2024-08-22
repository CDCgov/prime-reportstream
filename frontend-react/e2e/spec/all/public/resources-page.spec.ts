import { DeveloperResourcesPage } from "../../../pages/public/resources";
import { test as baseTest, expect } from "../../../test";

export interface DeveloperResourcesPageFixtures {
    developerResourcesPage: DeveloperResourcesPage;
}

const cards = [
    {
        name: "API guide",
    },
    {
        name: "GitHub",
    },
    {
        name: "Release notes",
    },
];

const test = baseTest.extend<DeveloperResourcesPageFixtures>({
    developerResourcesPage: async (
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
        const page = new DeveloperResourcesPage({
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

test.describe("Developer Resources page", () => {
    test.describe("Header", () => {
        test("has correct title + heading", async ({ developerResourcesPage }) => {
            await developerResourcesPage.testHeader();
        });
    });

    test.describe("CTA", () => {
        for (const card of cards) {
            test(`should have ${card.name}`, async ({ developerResourcesPage }) => {
                const cardHeader = developerResourcesPage.page.locator(".usa-card__header", {
                    hasText: card.name,
                });

                await expect(cardHeader).toBeVisible();
            });
        }
    });

    test.describe("Footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({ developerResourcesPage }) => {
            await developerResourcesPage.testFooter();
        });
    });
});
