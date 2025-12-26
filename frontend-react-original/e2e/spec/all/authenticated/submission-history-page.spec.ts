import { expect } from "@playwright/test";

import { noData, tableDataCellValue, tableRows } from "../../../helpers/utils";
import * as submissionHistory from "../../../pages/authenticated/submission-history";
import { id, openReportIdDetailPage, SubmissionHistoryPage } from "../../../pages/authenticated/submission-history";
import { test as baseTest, logins } from "../../../test";

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

test.describe("Submission history page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ submissionHistoryPage }) => {
            await expect(submissionHistoryPage.page).toHaveURL("/login");
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await submissionHistory.goto(page);
            });

            test("will not load page", async ({ page }) => {
                await expect(page.getByText("Cannot fetch Organization data as admin")).toBeVisible();
            });

            test("has footer", async ({ submissionHistoryPage }) => {
                await expect(submissionHistoryPage.footer).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test("nav contains the 'Submission History' option", async ({ submissionHistoryPage }) => {
                const navItems = submissionHistoryPage.page.locator(".usa-nav  li");
                await expect(navItems).toContainText(["Submission History"]);
            });

            test("has correct title", async ({ submissionHistoryPage }) => {
                await expect(submissionHistoryPage.page).toHaveTitle(/Submission history/);
            });

            test("has filter", async ({ submissionHistoryPage }) => {
                await expect(submissionHistoryPage.page.getByTestId("filter-container")).toBeAttached();
            });

            test.describe("table", () => {
                test("table has correct headers", async ({ submissionHistoryPage }) => {
                    await submissionHistory.tableHeaders(submissionHistoryPage.page);
                });

                test("table column 'ReportId' will open the report details", async ({ submissionHistoryPage }) => {
                    const reportId = tableRows(submissionHistoryPage.page).nth(0).locator("td").nth(0);
                    await expect(reportId).toContainText(id);
                    await reportId.getByRole("link", { name: id }).click();

                    await openReportIdDetailPage(submissionHistoryPage.page, id);
                });

                test("table column 'Date/time submitted' has expected data", async ({ submissionHistoryPage }) => {
                    expect(await tableDataCellValue(submissionHistoryPage.page, 0, 1)).toEqual("3/7/2024, 6:00:22 PM");
                });

                test("table column 'File' has expected data", async ({ submissionHistoryPage }) => {
                    expect(await tableDataCellValue(submissionHistoryPage.page, 0, 2)).toEqual("myfile.hl7");
                    expect(await tableDataCellValue(submissionHistoryPage.page, 1, 2)).toEqual(
                        "None-03c3b7ab-7c65-4174-bea7-9195cbb7ed01-20240314174050.hl7",
                    );
                });

                test("table column 'Records' has expected data", async ({ submissionHistoryPage }) => {
                    expect(await tableDataCellValue(submissionHistoryPage.page, 0, 3)).toEqual("1");
                });

                test("table column 'Status' has expected data", async ({ submissionHistoryPage }) => {
                    expect(await tableDataCellValue(submissionHistoryPage.page, 0, 4)).toEqual("Success");
                });

                test("table has pagination", async ({ submissionHistoryPage }) => {
                    await expect(submissionHistoryPage.page.getByTestId("Submissions pagination")).toBeAttached();
                });
            });

            test("has footer", async ({ submissionHistoryPage }) => {
                await expect(submissionHistoryPage.footer).toBeAttached();
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: logins.receiver.path });

        test("nav does not contain the Submissions option", async ({ submissionHistoryPage }) => {
            const navItems = submissionHistoryPage.page.locator(".usa-nav  li");
            await expect(navItems).not.toContainText(["Submissions"]);
        });

        test("displays no data message", async ({ submissionHistoryPage }) => {
            await expect(noData(submissionHistoryPage.page)).toBeAttached();
        });

        test("has correct title", async ({ submissionHistoryPage }) => {
            await expect(submissionHistoryPage.page).toHaveTitle(/Submission history/);
        });

        test("has footer", async ({ submissionHistoryPage }) => {
            await expect(submissionHistoryPage.footer).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: logins.sender.path });

        test("nav contains the Submission History option", async ({ submissionHistoryPage }) => {
            const navItems = submissionHistoryPage.page.locator(".usa-nav  li");
            await expect(navItems).toContainText(["Submission History"]);
        });

        test("has correct title", async ({ submissionHistoryPage }) => {
            await expect(submissionHistoryPage.page).toHaveTitle(/Submission history/);
        });

        test("has filter", async ({ submissionHistoryPage }) => {
            await expect(submissionHistoryPage.page.getByTestId("filter-container")).toBeAttached();
        });

        test.describe("table", () => {
            test("table has correct headers", async ({ submissionHistoryPage }) => {
                await submissionHistory.tableHeaders(submissionHistoryPage.page);
            });

            test("table column 'ReportId' will open the report details", async ({ submissionHistoryPage }) => {
                const reportId = tableRows(submissionHistoryPage.page).nth(0).locator("td").nth(0);
                await expect(reportId).toContainText(id);
                await reportId.getByRole("link", { name: id }).click();

                await openReportIdDetailPage(submissionHistoryPage.page, id);
            });

            test("table column 'Date/time submitted' has expected data", async ({ submissionHistoryPage }) => {
                expect(await tableDataCellValue(submissionHistoryPage.page, 0, 1)).toEqual("3/7/2024, 6:00:22 PM");
            });

            test("table column 'Records' has expected data", async ({ submissionHistoryPage }) => {
                expect(await tableDataCellValue(submissionHistoryPage.page, 0, 3)).toEqual("1");
            });

            test("table column 'Status' has expected data", async ({ submissionHistoryPage }) => {
                expect(await tableDataCellValue(submissionHistoryPage.page, 0, 4)).toEqual("Success");
            });

            test("table has pagination", async ({ submissionHistoryPage }) => {
                await expect(submissionHistoryPage.page.getByTestId("Submissions pagination")).toBeAttached();
            });
        });

        test("has footer", async ({ submissionHistoryPage }) => {
            await expect(submissionHistoryPage.footer).toBeAttached();
        });
    });
});
