import { expect } from "@playwright/test";

import {
    FALLBACK_FROM_DATE_STRING,
    FALLBACK_TO_DATE_STRING,
} from "../../../../src/hooks/filters/UseDateRange/UseDateRange";
import {
    tableColumnDateTimeInRange,
    tableDataCellValue,
    TEST_ORG_ELIMS_RECEIVER_ELIMS,
    TEST_ORG_IGNORE,
    TEST_ORG_UP_RECEIVER_UP,
} from "../../../helpers/utils";
import { endDate, setDate, startDate } from "../../../pages/authenticated/daily-data";
import * as submissionHistory from "../../../pages/authenticated/submission-history";
import { openReportIdDetailPage, SubmissionHistoryPage } from "../../../pages/authenticated/submission-history";
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
// const SMOKE_RECEIVERS = [TEST_ORG_UP_RECEIVER_UP, TEST_ORG_ELIMS_RECEIVER_ELIMS];

test.describe(
    "Submission history page - user flow smoke tests",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("admin user", () => {
            test.use({ storageState: "e2e/.auth/admin.json" });

            test.beforeAll(({ browserName }) => {
                test.skip(browserName !== "chromium");
            });

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

                    test.describe("on 'Apply'", () => {
                        test("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
                            submissionHistoryPage,
                        }) => {
                            const fromDate = await setDate(submissionHistoryPage.page, "#start-date", 180);
                            const toDate = await setDate(submissionHistoryPage.page, "#end-date", 0);

                            // Apply button is enabled
                            await submissionHistoryPage.filterButton.click();
                            await submissionHistoryPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                            // Check that table data contains the dates/times that were selected
                            const areDatesInRange = await tableColumnDateTimeInRange(
                                submissionHistoryPage.page,
                                1,
                                fromDate,
                                toDate,
                            );
                            expect(areDatesInRange).toBe(true);
                        });
                    });
                });
            });
        });
    },
);
