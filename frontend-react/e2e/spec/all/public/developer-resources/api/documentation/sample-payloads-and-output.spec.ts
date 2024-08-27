import { scrollToFooter, scrollToTop } from "../../../../../../helpers/utils";
import { SamplePayloadsAndOutputs } from "../../../../../../pages/public/developer-resources/api/documentation/sample-payloads-and-output";
import { test as baseTest, expect } from "../../../../../../test";

export interface SecurityPageFixtures {
    samplePayloadsAndOutputs: SamplePayloadsAndOutputs;
}

const test = baseTest.extend<SecurityPageFixtures>({
    samplePayloadsAndOutputs: async (
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
        const page = new SamplePayloadsAndOutputs({
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
    "Developer Resources / API / Documentation / Sample payloads and output page",
    {
        tag: "@smoke",
    },
    () => {
        test("has correct title", async ({ samplePayloadsAndOutputs }) => {
            await expect(samplePayloadsAndOutputs.page).toHaveTitle(samplePayloadsAndOutputs.title);
            await expect(samplePayloadsAndOutputs.heading).toBeVisible();
        });

        test("has side nav", async ({ samplePayloadsAndOutputs }) => {
            await expect(
                samplePayloadsAndOutputs.page.getByRole("navigation", { name: "side-navigation" }),
            ).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ samplePayloadsAndOutputs }) => {
                await expect(samplePayloadsAndOutputs.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ samplePayloadsAndOutputs }) => {
                await expect(samplePayloadsAndOutputs.footer).not.toBeInViewport();
                await scrollToFooter(samplePayloadsAndOutputs.page);
                await expect(samplePayloadsAndOutputs.footer).toBeInViewport();
                await expect(samplePayloadsAndOutputs.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(samplePayloadsAndOutputs.page);
                await expect(samplePayloadsAndOutputs.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
