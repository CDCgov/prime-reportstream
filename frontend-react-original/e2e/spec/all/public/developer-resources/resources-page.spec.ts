import { DeveloperResourcesPage } from "../../../../pages/public/developer-resources/resources";
import { test as baseTest } from "../../../../test";

export interface DeveloperResourcesPageFixtures {
    developerResourcesPage: DeveloperResourcesPage;
}

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

const cards = [
    {
        name: "API onboarding guide",
    },
    {
        name: "GitHub",
    },
    {
        name: "Release notes",
    },
];

test.describe("Developer Resources page", () => {
    test.describe("Header", () => {
        test("has correct title + heading", async ({ developerResourcesPage }) => {
            await developerResourcesPage.testHeader();
        });
    });

    test.describe("CTA", () => {
        for (const card of cards) {
            test(`should have ${card.name}`, async ({ developerResourcesPage }) => {
                await developerResourcesPage.testCard(card);
            });
        }
    });

    test.describe("Footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({ developerResourcesPage }) => {
            await developerResourcesPage.testFooter();
        });
    });
});
