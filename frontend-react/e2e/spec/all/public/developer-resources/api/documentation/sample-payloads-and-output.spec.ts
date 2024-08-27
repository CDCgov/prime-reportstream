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

        test("has correct title + heading", async ({ samplePayloadsAndOutputs }) => {
            await samplePayloadsAndOutputs.testHeader();
        });

        test("footer", async ({ samplePayloadsAndOutputs }) => {
            await samplePayloadsAndOutputs.testFooter();
        });
    },
);
