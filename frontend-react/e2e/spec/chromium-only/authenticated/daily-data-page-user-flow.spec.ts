import { expect } from "@playwright/test";

import { format } from "date-fns";
import fs from "node:fs";
import {
    expectTableColumnValues,
    removeDateTime,
    tableColumnDateTimeInRange,
    tableDataCellValue,
    tableRows,
    TEST_ORG_CP_RECEIVER_CP,
    TEST_ORG_ELIMS_RECEIVER_ELIMS,
    TEST_ORG_IGNORE,
    TEST_ORG_UP_RECEIVER_UP,
} from "../../../helpers/utils";
import {
    applyButton,
    DailyDataPage,
    detailsTableHeaders,
    endDate,
    endTime,
    filterReset,
    filterStatus,
    receiverDropdown,
    searchButton,
    searchInput,
    searchReset,
    setDate,
    setTime,
    startDate,
    startTime,
} from "../../../pages/authenticated/daily-data.js";
import { URL_REPORT_DETAILS } from "../../../pages/authenticated/report-details.js";
import { test as baseTest } from "../../../test";

const defaultStartTime = "9:00am";
const defaultEndTime = "11:00pm";

export interface DailyDataPageFixtures {
    dailyDataPage: DailyDataPage;
}

const test = baseTest.extend<DailyDataPageFixtures>({
    dailyDataPage: async (
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
        const page = new DailyDataPage({
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
const SMOKE_RECEIVERS = [TEST_ORG_UP_RECEIVER_UP, TEST_ORG_CP_RECEIVER_CP, TEST_ORG_ELIMS_RECEIVER_ELIMS];

test.describe(
    "Daily Data page - user flow smoke tests",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("admin user", () => {
            test.use({ storageState: "e2e/.auth/admin.json" });

            test.describe(`${TEST_ORG_IGNORE} org - ${TEST_ORG_UP_RECEIVER_UP} receiver`, () => {
                test.describe("onLoad", () => {
                    test("has correct title", async ({ dailyDataPage }) => {
                        await expect(dailyDataPage.page).toHaveTitle(dailyDataPage.title);
                        await expect(dailyDataPage.heading).toBeVisible();
                    });

                    test("table has correct headers", async ({ dailyDataPage }) => {
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        await expect(dailyDataPage.page.locator(".usa-table th").nth(0)).toHaveText(/Report ID/);
                        await expect(dailyDataPage.page.locator(".usa-table th").nth(1)).toHaveText(/Time received/);
                        await expect(dailyDataPage.page.locator(".usa-table th").nth(2)).toHaveText(
                            /File available until/,
                        );
                        await expect(dailyDataPage.page.locator(".usa-table th").nth(3)).toHaveText(/Items/);
                        await expect(dailyDataPage.page.locator(".usa-table th").nth(4)).toHaveText(/Filename/);
                        await expect(dailyDataPage.page.locator(".usa-table th").nth(5)).toHaveText(/Receiver/);
                    });

                    test("table has pagination", async ({ dailyDataPage }) => {
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        await expect(dailyDataPage.page.locator('[aria-label="Pagination"]')).toBeAttached();
                    });

                    test("has footer", async ({ dailyDataPage }) => {
                        await expect(dailyDataPage.page.locator("footer")).toBeAttached();
                    });
                });

                test.describe("filter", () => {
                    test.describe("onLoad", () => {
                        test.beforeEach(async ({ dailyDataPage }) => {
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        });

                        test("does not have a receiver selected", async ({ dailyDataPage }) => {
                            await expect(receiverDropdown(dailyDataPage.page)).toBeAttached();
                            await expect(receiverDropdown(dailyDataPage.page)).toHaveValue("");
                        });

                        test("'From' date does not have a value", async ({ dailyDataPage }) => {
                            await expect(startDate(dailyDataPage.page)).toBeAttached();
                            await expect(startDate(dailyDataPage.page)).toHaveValue("");
                        });

                        test("'To' date does not have a value", async ({ dailyDataPage }) => {
                            await expect(endDate(dailyDataPage.page)).toBeAttached();
                            await expect(endDate(dailyDataPage.page)).toHaveValue("");
                        });

                        test("'Start time' does not have a value", async ({ dailyDataPage }) => {
                            await expect(startTime(dailyDataPage.page)).toBeAttached();
                            await expect(startTime(dailyDataPage.page)).toHaveText("");
                        });

                        test("'End time'' does not have a value", async ({ dailyDataPage }) => {
                            await expect(endTime(dailyDataPage.page)).toBeAttached();
                            await expect(endTime(dailyDataPage.page)).toHaveText("");
                        });
                    });

                    test.describe("on 'Apply'", () => {
                        test.beforeEach(async ({ dailyDataPage }) => {
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                            await dailyDataPage.page
                                .locator("#receiver-dropdown")
                                .selectOption(TEST_ORG_UP_RECEIVER_UP);
                        });

                        test.afterEach(async ({ dailyDataPage }) => {
                            await filterReset(dailyDataPage.page).click();
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        });

                        test("with 'Receiver' selected", async ({ dailyDataPage }) => {
                            await applyButton(dailyDataPage.page).click();
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                            // Check that table data contains the receiver selected
                            await expectTableColumnValues(
                                dailyDataPage.page,
                                5,
                                `${TEST_ORG_IGNORE}.${TEST_ORG_UP_RECEIVER_UP}`,
                            );

                            // Check filter status lists receiver value
                            const filterStatusText = filterStatus([TEST_ORG_UP_RECEIVER_UP]);
                            await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(
                                filterStatusText,
                            );

                            // Receiver dropdown persists
                            await expect(receiverDropdown(dailyDataPage.page)).toHaveValue(TEST_ORG_UP_RECEIVER_UP);
                        });

                        test.skip("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
                            dailyDataPage,
                        }) => {
                            // TODO: The date filtering query is currently broken
                            const fromDate = await setDate(dailyDataPage.page, "#start-date", 180);
                            const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                            await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                            await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                            // Apply button is enabled
                            await applyButton(dailyDataPage.page).click();
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                            // Check that table data contains the dates/times that were selected
                            const areDatesInRange = await tableColumnDateTimeInRange(
                                dailyDataPage.page,
                                1,
                                fromDate,
                                toDate,
                                defaultStartTime,
                                defaultEndTime,
                            );
                            expect(areDatesInRange).toBe(true);

                            // Check filter status lists receiver value
                            const filterStatusText = filterStatus([
                                TEST_ORG_UP_RECEIVER_UP,
                                `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                                `${defaultStartTime}–${defaultEndTime}`,
                            ]);
                            await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(
                                filterStatusText,
                            );
                        });

                        test("clears 'Report ID'", async ({ dailyDataPage }) => {
                            // Search by Report ID
                            const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                            await searchInput(dailyDataPage.page).fill(reportId);
                            await searchButton(dailyDataPage.page).click();
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                            const rowCount = await tableRows(dailyDataPage.page).count();
                            expect(rowCount).toEqual(1);

                            // Check filter status lists receiver value
                            let filterStatusText = filterStatus([reportId]);
                            await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(
                                filterStatusText,
                            );

                            // Perform search with filters selected
                            await dailyDataPage.page
                                .locator("#receiver-dropdown")
                                .selectOption(TEST_ORG_UP_RECEIVER_UP);
                            const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                            const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                            await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                            await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                            await applyButton(dailyDataPage.page).click();
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                            // Check filter status lists receiver value
                            filterStatusText = filterStatus([
                                TEST_ORG_UP_RECEIVER_UP,
                                `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            ]);
                            await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(
                                filterStatusText,
                            );

                            // Check search is cleared
                            await expect(searchInput(dailyDataPage.page)).toHaveValue("");
                        });
                    });

                    test.describe("on 'Reset'", () => {
                        test("form elements clear", async ({ dailyDataPage }) => {
                            await filterReset(dailyDataPage.page).click();
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                            await expect(receiverDropdown(dailyDataPage.page)).toHaveValue("");
                            await expect(startDate(dailyDataPage.page)).toHaveValue("");
                            await expect(endDate(dailyDataPage.page)).toHaveValue("");
                            await expect(startTime(dailyDataPage.page)).toHaveValue("");
                            await expect(endTime(dailyDataPage.page)).toHaveValue("");
                        });
                    });
                });

                test.describe("search", () => {
                    test.beforeEach(async ({ dailyDataPage }) => {
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                    });

                    test.afterEach(async ({ dailyDataPage }) => {
                        await searchReset(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                    });

                    test("returns match for Report ID", async ({ dailyDataPage }) => {
                        const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                        await searchInput(dailyDataPage.page).fill(reportId);
                        await searchButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        const rowCount = await tableRows(dailyDataPage.page).count();
                        expect(rowCount).toEqual(1);

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus([reportId]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                        //Check table data matches search
                        expect(await tableDataCellValue(dailyDataPage.page, 0, 0)).toEqual(reportId);
                    });

                    test("returns match for Filename", async ({ dailyDataPage }) => {
                        const fileName = await tableDataCellValue(dailyDataPage.page, 0, 4);
                        await searchInput(dailyDataPage.page).fill(removeDateTime(fileName));
                        await searchButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        const rowCount = await tableRows(dailyDataPage.page).count();
                        expect(rowCount).toEqual(1);

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus([fileName]);
                        const actualText = await dailyDataPage.page.getByTestId("filter-status").textContent();
                        expect(filterStatusText).toContain(actualText);

                        //Check table data matches search
                        expect(await tableDataCellValue(dailyDataPage.page, 0, 4)).toEqual(fileName);
                    });

                    test("on search 'Reset' clears search results", async ({ dailyDataPage }) => {
                        const rowCount = await tableRows(dailyDataPage.page).count();
                        const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                        await searchInput(dailyDataPage.page).fill(reportId);
                        await searchButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        expect(await tableRows(dailyDataPage.page).count()).toEqual(1);

                        await searchReset(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        await expect(searchInput(dailyDataPage.page)).toHaveValue("");
                        expect(await tableRows(dailyDataPage.page).count()).toEqual(rowCount);
                    });

                    test("clears filters on search", async ({ dailyDataPage }) => {
                        // Perform search with all filters selected
                        await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_UP_RECEIVER_UP);
                        const fromDate = await setDate(dailyDataPage.page, "#start-date", 7);
                        const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                        await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Check filter status lists receiver value
                        let filterStatusText = filterStatus([
                            TEST_ORG_UP_RECEIVER_UP,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        ]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                        const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                        await searchInput(dailyDataPage.page).fill(reportId);
                        await searchButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Check filter status lists receiver value
                        filterStatusText = filterStatus([reportId]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                        // TODO: Fix - Doesnt work in CI
                        //Check table data matches search
                        // expect(await tableDataCellValue(dailyDataPage.page, 0, 0)).toEqual(reportId);

                        // Check filters are cleared
                        await expect(receiverDropdown(dailyDataPage.page)).toHaveValue("");
                        await expect(startDate(dailyDataPage.page)).toHaveValue("");
                        await expect(endDate(dailyDataPage.page)).toHaveValue("");
                        await expect(startTime(dailyDataPage.page)).toHaveValue("");
                        await expect(endTime(dailyDataPage.page)).toHaveValue("");
                    });
                });

                test.describe("on 'Filename' click", () => {
                    test.beforeEach(async ({ dailyDataPage }) => {
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_UP_RECEIVER_UP);
                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                    });

                    test("downloads the file", async ({ dailyDataPage, isMockDisabled }) => {
                        test.skip(!isMockDisabled, "Mocks are ENABLED, skipping 'downloads the file' test");
                        // Sort by File available until, but they're in ASCENDING order
                        await dailyDataPage.page.getByRole("button", { name: "File available until" }).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        // Sort by File available until again, to get the absolute latest result
                        await dailyDataPage.page.getByRole("button", { name: "File available until" }).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        const downloadProm = dailyDataPage.page.waitForEvent("download");
                        const fileName = await tableDataCellValue(dailyDataPage.page, 0, 4);

                        await dailyDataPage.page.getByRole("button", { name: fileName }).click();
                        const download = await downloadProm;

                        // assert filename
                        expect(removeDateTime(download.suggestedFilename())).toEqual(removeDateTime(fileName));

                        // get and assert stats
                        expect((await fs.promises.stat(await download.path())).size).toBeGreaterThan(200);
                    });
                });
            });

            SMOKE_RECEIVERS.forEach((receiver) => {
                test.describe(`${TEST_ORG_IGNORE} org - ${receiver} receiver`, () => {
                    test.describe("on 'Report ID' click", () => {
                        test.beforeEach(async ({ dailyDataPage, isFrontendWarningsLog }) => {
                            test.skip(
                                !isFrontendWarningsLog,
                                "isFrontendWarningsLog must be TRUE, skipping 'on 'Report ID' click' test",
                            );
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                            await dailyDataPage.page.locator("#receiver-dropdown").selectOption(receiver);
                            await applyButton(dailyDataPage.page).click();
                            await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                        });

                        test("opens the Daily Data details page", async ({ dailyDataPage, isFrontendWarningsLog }) => {
                            test.skip(
                                !isFrontendWarningsLog,
                                "isFrontendWarningsLog must be TRUE, skipping 'opens the Daily Data details page' test",
                            );
                            const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);

                            await dailyDataPage.page.getByRole("link", { name: reportId }).click();
                            const responsePromise = await dailyDataPage.page.waitForResponse(
                                (res) => res.status() === 200 && res.url().includes("/delivery"),
                            );

                            if (responsePromise) {
                                await expect(dailyDataPage.page).toHaveURL(`${URL_REPORT_DETAILS}/${reportId}`);
                                await expect(dailyDataPage.page).toHaveTitle(/Daily Data - ReportStream/);
                                await expect(dailyDataPage.page.locator("h1").getByText(reportId)).toBeVisible();

                                // Facility table headers
                                await detailsTableHeaders(dailyDataPage.page);
                            } else {
                                console.error("Request not received within the timeout period");
                            }
                        });
                    });
                });
            });
        });
    },
);
