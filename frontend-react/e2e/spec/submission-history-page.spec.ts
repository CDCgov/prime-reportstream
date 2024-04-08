import { expect, test } from "@playwright/test";

import { selectTestOrg, tableData, TEST_ORG_IGNORE } from "../helpers/utils";
import { reportIdDetailPage } from "../pages/report-details";
import * as submissionHistory from "../pages/submission-history";

const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";
test.describe("Submission history page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await submissionHistory.goto(page);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await submissionHistory.goto(page);
            });

            test("will not load page", async ({ page }) => {
                await expect(
                    page.getByText("Cannot fetch Organization data as admin"),
                ).toBeVisible();
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test.beforeEach(async ({ page }) => {
                await selectTestOrg(page);
                await submissionHistory.mockGetSubmissionsResponse(
                    page,
                    TEST_ORG_IGNORE,
                );
                await submissionHistory.mockGetReportHistoryResponse(page);
                // abort all app insight calls
                await page.route("**/v2/track", (route) => route.abort());
                await submissionHistory.goto(page);
            });

            test("nav contains the 'Submissions' option", async ({ page }) => {
                const navItems = page.locator(".usa-nav  li");
                await expect(navItems).toContainText(["Submissions"]);
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveTitle(/Submission history/);
            });

            test("has filter", async ({ page }) => {
                await expect(
                    page.getByTestId("filter-container"),
                ).toBeAttached();
            });

            test.describe("table", () => {
                test("table has correct headers", async ({ page }) => {
                    await submissionHistory.tableHeaders(page);
                });

                test("table column 'ReportId' will open the report details", async ({
                    page,
                }) => {
                    const reportId = page
                        .locator(".usa-table tbody")
                        .locator("tr")
                        .nth(0)
                        .locator("td")
                        .nth(0);
                    await expect(reportId).toContainText(id);
                    await reportId.click();

                    const submissionsDetail = await reportIdDetailPage(page);
                    await expect(submissionsDetail).toHaveURL(
                        `/submissions/${id}`,
                    );
                    expect(
                        submissionsDetail.getByText(`Report ID:${id}`),
                    ).toBeTruthy();
                });

                test("table column 'Date/time submitted' has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 1, "3/7/2024, 6:00:22 PM");
                });

                test("table column 'Records' has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 3, "1");
                });

                test("table column 'Status' has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 4, "Success");
                });

                test("table has pagination", async ({ page }) => {
                    await expect(
                        page.getByTestId("Submissions pagination"),
                    ).toBeAttached();
                });
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await submissionHistory.goto(page);
        });

        test("nav does not contain the Submissions option", async ({
            page,
        }) => {
            const navItems = page.locator(".usa-nav  li");
            await expect(navItems).not.toContainText(["Submissions"]);
        });

        test("displays no data message", async ({ page }) => {
            await expect(page.getByText(/No available data/)).toBeAttached();
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Submission history/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await submissionHistory.mockGetSubmissionsResponse(
                page,
                TEST_ORG_IGNORE,
            );
            await submissionHistory.mockGetReportHistoryResponse(page);
            await submissionHistory.goto(page);
        });

        test("nav contains the Submissions option", async ({ page }) => {
            const navItems = page.locator(".usa-nav  li");
            await expect(navItems).toContainText(["Submissions"]);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Submission history/);
        });

        test("has filter", async ({ page }) => {
            await expect(page.getByTestId("filter-container")).toBeAttached();
        });

        test.describe("table", () => {
            test("table has correct headers", async ({ page }) => {
                await submissionHistory.tableHeaders(page);
            });

            test("table column 'ReportId' will open the report details", async ({
                page,
            }) => {
                const reportId = page
                    .locator(".usa-table tbody")
                    .locator("tr")
                    .nth(0)
                    .locator("td")
                    .nth(0);
                await expect(reportId).toContainText(id);
                await reportId.click();

                const submissionsDetail = await reportIdDetailPage(page);
                await expect(submissionsDetail).toHaveURL(`/submissions/${id}`);
                expect(
                    submissionsDetail.getByText(`Report ID:${id}`),
                ).toBeTruthy();
            });

            test("table column 'Date/time submitted' has expected data", async ({
                page,
            }) => {
                await tableData(page, 0, 1, "3/7/2024, 6:00:22 PM");
            });

            test("table column 'Records' has expected data", async ({
                page,
            }) => {
                await tableData(page, 0, 3, "1");
            });

            test("table column 'Status' has expected data", async ({
                page,
            }) => {
                await tableData(page, 0, 4, "Success");
            });

            test("table has pagination", async ({ page }) => {
                await expect(
                    page.getByTestId("Submissions pagination"),
                ).toBeAttached();
            });
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });
});
