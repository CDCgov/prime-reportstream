import { scrollToFooter, scrollToTop } from "../../../../../helpers/utils";
import { ResponsesFromReportStreamPage } from "../../../../../pages/developer-resources/api/documentation/responses-from-reportstream";
import { test as baseTest, expect } from "../../../../../test";

export interface SecurityPageFixtures {
    responsesFromReportStreamPage: ResponsesFromReportStreamPage;
}

const test = baseTest.extend<SecurityPageFixtures>({
    responsesFromReportStreamPage: async (
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
        const page = new ResponsesFromReportStreamPage({
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
    "Developer Resources / API / Documentation / Responses From ReportStream page",
    {
        tag: "@smoke",
    },
    () => {
        test("has correct title", async ({ responsesFromReportStreamPage }) => {
            await expect(responsesFromReportStreamPage.page).toHaveTitle(responsesFromReportStreamPage.title);
            await expect(responsesFromReportStreamPage.heading).toBeVisible();
        });

        test("has side nav", async ({ responsesFromReportStreamPage }) => {
            await expect(
                responsesFromReportStreamPage.page.getByRole("navigation", { name: "side-navigation" }),
            ).toBeVisible();
        });

        test.describe("Footer", () => {
            test("has footer", async ({ responsesFromReportStreamPage }) => {
                await expect(responsesFromReportStreamPage.footer).toBeAttached();
            });

            test("explicit scroll to footer and then scroll to top", async ({ responsesFromReportStreamPage }) => {
                await expect(responsesFromReportStreamPage.footer).not.toBeInViewport();
                await scrollToFooter(responsesFromReportStreamPage.page);
                await expect(responsesFromReportStreamPage.footer).toBeInViewport();
                await expect(responsesFromReportStreamPage.page.getByTestId("govBanner")).not.toBeInViewport();
                await scrollToTop(responsesFromReportStreamPage.page);
                await expect(responsesFromReportStreamPage.page.getByTestId("govBanner")).toBeInViewport();
            });
        });
    },
);
