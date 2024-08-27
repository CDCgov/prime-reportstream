import { aboutSideNav } from "../../../../helpers/internal-links";
import { RoadmapPage } from "../../../../pages/public/about/roadmap";
import { test as baseTest } from "../../../../test";

export interface RoadmapPageFixtures {
    roadmapPage: RoadmapPage;
}

const test = baseTest.extend<RoadmapPageFixtures>({
    roadmapPage: async (
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
        const page = new RoadmapPage({
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
        name: "News",
    },
    {
        name: "Release notes",
    },
    {
        name: "Developer resources",
    },
];

test.describe(
    "Product roadmap page",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("Header", () => {
            test("has correct title + heading", async ({ roadmapPage }) => {
                await roadmapPage.testHeader();
            });
        });

        test.describe("Side navigation", () => {
            test("has correct About sidenav items", async ({ roadmapPage }) => {
                await roadmapPage.testSidenav(aboutSideNav);
            });
        });

        test.describe("CTA", () => {
            for (const card of cards) {
                test(`should have ${card.name}`, async ({ roadmapPage }) => {
                    await roadmapPage.testCard(card);
                });
            }
        });

        test.describe("Footer", () => {
            test("has footer and explicit scroll to footer and scroll to top", async ({ roadmapPage }) => {
                await roadmapPage.testFooter();
            });
        });
    },
);
