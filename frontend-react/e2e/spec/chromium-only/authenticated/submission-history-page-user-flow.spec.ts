import { expect } from "@playwright/test";

import {
    FALLBACK_FROM_DATE_STRING,
    FALLBACK_TO_DATE_STRING,
} from "../../../../src/hooks/filters/UseDateRange/UseDateRange";
import { noData, tableDataCellValue, TEST_ORG_IGNORE } from "../../../helpers/utils";
import { endDate, setDate, startDate } from "../../../pages/authenticated/daily-data";
import * as submissionHistory from "../../../pages/authenticated/submission-history";
import {
    openReportIdDetailPage,
    SubmissionHistoryPage,
    URL_SUBMISSION_HISTORY,
} from "../../../pages/authenticated/submission-history";
import { test as baseTest } from "../../../test";

export interface SubmissionHistoryPageFixtures {
    submissionHistoryPage: SubmissionHistoryPage;
}

const test = baseTest.extend<SubmissionHistoryPageFixtures>({
    submissionHistoryPage: async (
        {
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
        },
        use,
    ) => {
        const page = new SubmissionHistoryPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
            isTestOrg: true,
        });
        await page.goto();
        await use(page);
    },
});

test.describe(
    "Submission history page - user flow smoke tests",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("admin user", () => {
            test.use({ storageState: "e2e/.auth/admin.json" });

            test.describe(`${TEST_ORG_IGNORE} org`, () => {
                test("nav contains the 'Submission History' option", async ({ submissionHistoryPage }) => {
                    const navItems = submissionHistoryPage.page.locator(".usa-nav  li");
                    await expect(navItems).toContainText(["Submission History"]);
                });

                test("has correct title", async ({ submissionHistoryPage }) => {
                    await expect(submissionHistoryPage.page).toHaveTitle(submissionHistoryPage.title);
                    await expect(submissionHistoryPage.heading).toBeVisible();
                });

                test("has filter", async ({ submissionHistoryPage }) => {
                    await expect(submissionHistoryPage.page.getByTestId("filter-container")).toBeAttached();
                });

                test("has footer", async ({ submissionHistoryPage }) => {
                    await expect(submissionHistoryPage.footer).toBeAttached();
                });

                test.describe("table", () => {
                    test.beforeEach(async ({ submissionHistoryPage }) => {
                        await submissionHistoryPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                    });

                    test("table has correct headers", async ({ submissionHistoryPage }) => {
                        await submissionHistory.tableHeaders(submissionHistoryPage.page);
                    });

                    test("table column 'ReportId' will open the report details", async ({ submissionHistoryPage }) => {
                        const reportId = await tableDataCellValue(submissionHistoryPage.page, 0, 0);

                        await submissionHistoryPage.page.getByRole("link", { name: reportId }).click();
                        const responsePromise = await submissionHistoryPage.page.waitForResponse(
                            (res) => res.status() === 200 && res.url().includes("/history"),
                        );

                        if (responsePromise) {
                            await openReportIdDetailPage(submissionHistoryPage.page, reportId);
                        } else {
                            console.error("Request not received within the timeout period");
                        }
                    });

                    test("table has pagination", async ({ submissionHistoryPage }) => {
                        await expect(submissionHistoryPage.page.getByTestId("Submissions pagination")).toBeAttached();
                    });
                });

                test.describe("filter", () => {
                    test.describe("on 'onLoad'", () => {
                        test("'From' date has a default value", async ({ submissionHistoryPage }) => {
                            await expect(startDate(submissionHistoryPage.page)).toBeAttached();
                            await expect(startDate(submissionHistoryPage.page)).toHaveValue(FALLBACK_FROM_DATE_STRING);
                        });

                        test("'To' date has a default value", async ({ submissionHistoryPage }) => {
                            await expect(endDate(submissionHistoryPage.page)).toBeAttached();
                            await expect(endDate(submissionHistoryPage.page)).toHaveValue(FALLBACK_TO_DATE_STRING);
                        });
                    });

                    test.describe("on 'Filter to incorrect date'", () => {
                        test("with 'From' date, 'To' date", async ({ submissionHistoryPage, isMockDisabled }) => {
                            test.skip(
                                !isMockDisabled,
                                "Mocks are ENABLED, skipping 'on 'Filter to incorrect date' test",
                            );
                            const earliestDate = new Date(FALLBACK_FROM_DATE_STRING);
                            const currentDate = new Date();
                            const diffInTime = currentDate.getTime() - earliestDate.getTime();
                            const diffInDays = Math.floor(diffInTime / (1000 * 60 * 60 * 24));
                            await setDate(submissionHistoryPage.page, "#start-date", diffInDays);
                            await setDate(submissionHistoryPage.page, "#end-date", diffInDays - 1);

                            // Apply button is enabled
                            await submissionHistoryPage.filterButton.click();
                            const responsePromise = await submissionHistoryPage.page.waitForResponse(
                                (res) => res.status() === 200 && res.url().includes(URL_SUBMISSION_HISTORY),
                            );

                            if (responsePromise) {
                                await expect(noData(submissionHistoryPage.page)).toBeAttached();
                            } else {
                                console.error("Request not received within the timeout period");
                            }
                        });

                        test("on 'clear' resets the dates", async ({ submissionHistoryPage }) => {
                            await expect(startDate(submissionHistoryPage.page)).toHaveValue(FALLBACK_FROM_DATE_STRING);
                            await expect(endDate(submissionHistoryPage.page)).toHaveValue(FALLBACK_TO_DATE_STRING);

                            await setDate(submissionHistoryPage.page, "#start-date", 14);
                            await setDate(submissionHistoryPage.page, "#end-date", 14);

                            await submissionHistoryPage.clearButton.click();

                            await expect(startDate(submissionHistoryPage.page)).toHaveValue(FALLBACK_FROM_DATE_STRING);
                            await expect(endDate(submissionHistoryPage.page)).toHaveValue(FALLBACK_TO_DATE_STRING);
                        });
                    });
                });
            });
        });
    },
);
