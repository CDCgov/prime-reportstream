import { aboutSideNav } from "../../../../helpers/internal-links";
import { OurNetworkPage } from "../../../../pages/public/about/our-network";
import { test as baseTest } from "../../../../test";

export interface OurNetworkPageFixtures {
    ourNetworkPage: OurNetworkPage;
}

const test = baseTest.extend<OurNetworkPageFixtures>({
    ourNetworkPage: async (
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
        const page = new OurNetworkPage({
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
    "Our network page",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("Header", () => {
            test("has correct title + heading", async ({ ourNetworkPage }) => {
                await ourNetworkPage.testHeader();
            });
        });

        test.describe("Side navigation", () => {
            test("has correct About sidenav items", async ({ ourNetworkPage }) => {
                await ourNetworkPage.testSidenav(aboutSideNav);
            });
        });

        test.describe("Footer", () => {
            test("has footer and explicit scroll to footer and scroll to top", async ({ ourNetworkPage }) => {
                await ourNetworkPage.testFooter();
            });
        });
    },
);
