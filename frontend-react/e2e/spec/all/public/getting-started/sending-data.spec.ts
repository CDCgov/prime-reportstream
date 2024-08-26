import site from "../../../../../src/content/site.json" assert { type: "json" };
import { SendingDataPage } from "../../../../pages/public/getting-started/sending-data.js";
import { test as baseTest, expect } from "../../../../test";

export interface SendingDataPageFixtures {
    sendingDataPage: SendingDataPage;
}

const test = baseTest.extend<SendingDataPageFixtures>({
    sendingDataPage: async (
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
        const page = new SendingDataPage({
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

test.describe("Sending data page", () => {
    test.describe("Header", () => {
        test("has correct title + heading", async ({ sendingDataPage }) => {
            await sendingDataPage.testHeader();
        });
    });

    test("has link to get started with ReportStream", async ({ sendingDataPage }) => {
        const getStarted = sendingDataPage.page.locator(`a[href="${site.forms.connectWithRS.url}"]`).first();

        await getStarted.scrollIntoViewIfNeeded();
        await expect(getStarted).toBeVisible();
    });

    test.describe("onboarding at a glance section", () => {
        test("single accordion item", async ({ sendingDataPage }) => {
            const accordionItem = "accordionItem_1--content";
            await expect(sendingDataPage.page.getByTestId(accordionItem)).toBeHidden();

            await sendingDataPage.page
                .getByRole("button", {
                    name: "1. Gather your team",
                })
                .click();

            await expect(sendingDataPage.page.getByTestId(accordionItem)).toBeVisible();

            await sendingDataPage.page
                .getByRole("button", {
                    name: "1. Gather your team",
                })
                .click();

            await expect(sendingDataPage.page.getByTestId(accordionItem)).toBeHidden();
        });
    });

    test.describe("Footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({ sendingDataPage }) => {
            await sendingDataPage.testFooter();
        });
    });
});
