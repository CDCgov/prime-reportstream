import site from "../../../../../src/content/site.json" assert { type: "json" };
import { ReceivingDataPage } from "../../../../pages/public/getting-started/receiving-data";
import { test as baseTest, expect } from "../../../../test";

export interface ReceivingDataPageFixtures {
    receivingDataPage: ReceivingDataPage;
}

const test = baseTest.extend<ReceivingDataPageFixtures>({
    receivingDataPage: async (
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
        const page = new ReceivingDataPage({
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

test.describe("Receiving data page", () => {
    test.describe("Header", () => {
        test("has correct title + heading", async ({ receivingDataPage }) => {
            await receivingDataPage.testHeader();
        });
    });

    test("has link to onboarding form", async ({ receivingDataPage }) => {
        const getStarted = receivingDataPage.page.locator(`a[href="${site.forms.intakeElr.url}"]`).first();

        await getStarted.scrollIntoViewIfNeeded();
        await expect(getStarted).toBeVisible();
    });

    test.describe("onboarding at a glance section", () => {
        test("single accordion item", async ({ receivingDataPage }) => {
            const accordionItem = "accordionItem_onboarding-1--content";
            await expect(receivingDataPage.page.getByTestId(accordionItem)).toBeHidden();

            await receivingDataPage.page
                .getByRole("button", {
                    name: "1. Gather your team",
                })
                .click();

            await expect(receivingDataPage.page.getByTestId(accordionItem)).toBeVisible();

            await receivingDataPage.page
                .getByRole("button", {
                    name: "1. Gather your team",
                })
                .click();

            await expect(receivingDataPage.page.getByTestId(accordionItem)).toBeHidden();
        });
    });

    test.describe("Footer", () => {
        test("has footer and explicit scroll to footer and scroll to top", async ({ receivingDataPage }) => {
            await receivingDataPage.testFooter();
        });
    });
});
