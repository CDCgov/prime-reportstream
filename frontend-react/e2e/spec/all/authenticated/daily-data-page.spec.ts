import { expect } from "@playwright/test";

import { format } from "date-fns";
import {
    expectTableColumnValues,
    tableDataCellValue,
    tableRows,
    TEST_ORG_AK_RECEIVER,
    TEST_ORG_IGNORE,
    TEST_ORG_UP_RECEIVER_UP,
} from "../../../helpers/utils";
import * as dailyData from "../../../pages/authenticated/daily-data.js";
import {
    applyButton,
    DailyDataPage,
    endDate,
    endTime,
    endTimeClear,
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
    startTimeClear,
    tableHeaders,
} from "../../../pages/authenticated/daily-data.js";
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

test.describe("Daily Data page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page).toHaveURL("/login");
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        // TODO: cannot use dailyDataPage since we dont want the test org to be selected. Need a way to set the test org per test.
        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await dailyData.goto(page);
            });

            test("will not load page", async ({ page }) => {
                await expect(page.getByText("Cannot fetch Organization data as admin")).toBeVisible();
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test("nav contains the 'Daily Data' option", async ({ dailyDataPage }) => {
                const navItems = dailyDataPage.page.locator(".usa-nav  li");
                await expect(navItems).toContainText(["Daily Data"]);
            });

            test("has correct title", async ({ dailyDataPage }) => {
                await expect(dailyDataPage.page).toHaveTitle(dailyDataPage.title);
            });

            test("has receiver services dropdown", async ({ dailyDataPage }) => {
                await expect(dailyDataPage.page.locator("#receiver-dropdown")).toBeAttached();
            });

            test("has filter", async ({ dailyDataPage }) => {
                await expect(dailyDataPage.page.getByTestId("filter-form")).toBeAttached();
            });

            test("table has correct headers", async ({ dailyDataPage }) => {
                await expect(dailyDataPage.page.locator(".usa-table th").nth(0)).toHaveText(/Report ID/);
                await expect(dailyDataPage.page.locator(".usa-table th").nth(1)).toHaveText(/Time received/);
                await expect(dailyDataPage.page.locator(".usa-table th").nth(2)).toHaveText(/File available until/);
                await expect(dailyDataPage.page.locator(".usa-table th").nth(3)).toHaveText(/Items/);
                await expect(dailyDataPage.page.locator(".usa-table th").nth(4)).toHaveText(/Filename/);
                await expect(dailyDataPage.page.locator(".usa-table th").nth(5)).toHaveText(/Receiver/);
            });

            test("table has pagination", async ({ dailyDataPage }) => {
                await expect(dailyDataPage.page.locator('[aria-label="Pagination"]')).toBeAttached();
            });

            test("has footer", async ({ dailyDataPage }) => {
                await expect(dailyDataPage.page.locator("footer")).toBeAttached();
            });

            test.describe("filter", () => {
                test.describe("onLoad", () => {
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

                test.describe("with receiver selected", () => {
                    test.beforeEach(async ({ dailyDataPage }) => {
                        await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_UP_RECEIVER_UP);
                    });

                    test.afterEach(async ({ dailyDataPage }) => {
                        await filterReset(dailyDataPage.page).click();
                        await dailyDataPage.page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible", timeout: 60000 });
                    });

                    test("table loads with selected receiver data", async ({ dailyDataPage }) => {
                        await dailyDataPage.page
                            .getByRole("button", {
                                name: "Apply",
                            })
                            .click();
                        await dailyDataPage.page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible", timeout: 60000 });

                        // Check that table data contains the receiver selected
                        await expectTableColumnValues(
                            dailyDataPage.page,
                            5,
                            `${TEST_ORG_IGNORE}.${TEST_ORG_UP_RECEIVER_UP}`,
                        );

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus([TEST_ORG_UP_RECEIVER_UP]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                        // Receiver dropdown persists
                        await expect(receiverDropdown(dailyDataPage.page)).toHaveValue(TEST_ORG_UP_RECEIVER_UP);
                    });

                    test("with 'From' date", async ({ dailyDataPage }) => {
                        await expect(startDate(dailyDataPage.page)).toHaveValue("");

                        await setDate(dailyDataPage.page, "#start-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'To' date", async ({ dailyDataPage }) => {
                        await expect(endDate(dailyDataPage.page)).toHaveValue("");

                        await setDate(dailyDataPage.page, "#end-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'From' date and 'Start time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#start-date", 7);
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'From' date and 'End time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#start-date", 7);
                        await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'To' date and 'Start time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#end-date", 7);
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'To' date and 'End time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#end-date", 7);
                        await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'Start time' and 'End time'", async ({ dailyDataPage }) => {
                        // Start time
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                        await startTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                        await expect(applyButton(dailyDataPage.page)).toBeEnabled();

                        // End time
                        await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                        await endTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(dailyDataPage.page)).toBeEnabled();

                        // Start time and End time
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                        await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                        await startTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                        await endTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(dailyDataPage.page)).toBeEnabled();
                    });

                    test("with 'From' date and 'To' date", async ({ dailyDataPage }) => {
                        const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                        const toDate = await setDate(dailyDataPage.page, "#end-date", 0);

                        // Apply button is enabled
                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus([
                            TEST_ORG_UP_RECEIVER_UP,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        ]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'Start time'", async ({ dailyDataPage }) => {
                        const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                        const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                        // Apply button is enabled
                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                        await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                        await expect(dailyDataPage.page.locator("#start-time")).toHaveValue(defaultStartTime);

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus([
                            TEST_ORG_UP_RECEIVER_UP,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${defaultStartTime}–${"11:59pm"}`,
                        ]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'End time'", async ({ dailyDataPage }) => {
                        const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                        const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                        await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                        // Apply button is enabled
                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                        await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                        await expect(dailyDataPage.page.locator("#end-time")).toHaveValue(defaultEndTime);

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus([
                            TEST_ORG_UP_RECEIVER_UP,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${"12:00am"}–${defaultEndTime}`,
                        ]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#start-date", 0);
                        await setDate(dailyDataPage.page, "#end-date", 0);
                        await setTime(dailyDataPage.page, "#start-time", defaultEndTime);
                        await setTime(dailyDataPage.page, "#end-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });
                });

                test.describe("no receiver selected", () => {
                    test.beforeEach(async ({ dailyDataPage }) => {
                        await dailyDataPage.page.locator("#receiver-dropdown").selectOption("");
                    });

                    test.afterEach(async ({ dailyDataPage }) => {
                        await filterReset(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                    });

                    test("with 'From' date", async ({ dailyDataPage }) => {
                        await expect(startDate(dailyDataPage.page)).toHaveValue("");

                        await setDate(dailyDataPage.page, "#start-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'To' date", async ({ dailyDataPage }) => {
                        await expect(endDate(dailyDataPage.page)).toHaveValue("");

                        await setDate(dailyDataPage.page, "#end-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'From' date and 'Start time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#start-date", 7);
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'From' date and 'End time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#start-date", 7);
                        await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'To' date and 'Start time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#end-date", 7);
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'To' date and 'End time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#end-date", 7);
                        await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });

                    test("with 'Start time' and 'End time'", async ({ dailyDataPage }) => {
                        // Start time
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                        await startTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                        await expect(applyButton(dailyDataPage.page)).toBeDisabled();

                        // End time
                        await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                        await endTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(dailyDataPage.page)).toBeDisabled();

                        // Start time and End time
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                        await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                        await startTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                        await endTimeClear(dailyDataPage.page).click();
                        await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(dailyDataPage.page)).toBeDisabled();
                    });

                    test("with 'From' date and 'To' date", async ({ dailyDataPage }) => {
                        const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                        const toDate = await setDate(dailyDataPage.page, "#end-date", 0);

                        // Apply button is enabled
                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus([
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        ]);
                        await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'Start time'", async ({ dailyDataPage }) => {
                        const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                        const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                        await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                        // Apply button is enabled
                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                        await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                        await expect(dailyDataPage.page.locator("#start-time")).toHaveValue(defaultStartTime);
                    });

                    test("with 'From' date, 'To' date, 'End time'", async ({ dailyDataPage }) => {
                        const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                        const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                        await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                        // Apply button is enabled
                        await applyButton(dailyDataPage.page).click();
                        await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                        await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                        await expect(dailyDataPage.page.locator("#end-time")).toHaveValue(defaultEndTime);
                    });

                    test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({ dailyDataPage }) => {
                        await setDate(dailyDataPage.page, "#start-date", 0);
                        await setDate(dailyDataPage.page, "#end-date", 0);
                        await setTime(dailyDataPage.page, "#start-time", defaultEndTime);
                        await setTime(dailyDataPage.page, "#end-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    });
                });

                test.describe("on reset", () => {
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

                test("clears search on 'Apply'", async ({ dailyDataPage }) => {
                    // Search by Report ID
                    const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                    await searchInput(dailyDataPage.page).fill(reportId);
                    await searchButton(dailyDataPage.page).click();

                    // Check filter status lists receiver value
                    let filterStatusText = filterStatus([reportId]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                    // Perform search with filters selected
                    await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_UP_RECEIVER_UP);
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);

                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    filterStatusText = filterStatus([
                        TEST_ORG_UP_RECEIVER_UP,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                    // Check search is cleared
                    await expect(searchInput(dailyDataPage.page)).toHaveValue("");
                });
            });

            test.describe("search", () => {
                test("returns match for Report ID", async ({ dailyDataPage }) => {
                    const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                    await searchInput(dailyDataPage.page).fill(reportId);
                    await searchButton(dailyDataPage.page).click();

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
                    await searchInput(dailyDataPage.page).fill(fileName);
                    await searchButton(dailyDataPage.page).click();

                    const rowCount = await tableRows(dailyDataPage.page).count();
                    expect(rowCount).toEqual(1);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([fileName]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                    //Check table data matches search
                    expect(await tableDataCellValue(dailyDataPage.page, 0, 4)).toEqual(fileName);
                });

                test("on reset clears search results", async ({ dailyDataPage }) => {
                    const fileName = await tableDataCellValue(dailyDataPage.page, 0, 4);
                    await searchInput(dailyDataPage.page).fill(fileName);
                    await searchButton(dailyDataPage.page).click();

                    await searchReset(dailyDataPage.page).click();
                    await expect(searchInput(dailyDataPage.page)).toHaveValue("");
                });

                test("clears filters on search", async ({ dailyDataPage }) => {
                    // Perform search with all filters selected
                    await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_UP_RECEIVER_UP);
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    let filterStatusText = filterStatus([
                        TEST_ORG_UP_RECEIVER_UP,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${defaultEndTime}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                    const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                    await searchInput(dailyDataPage.page).fill(reportId);
                    await searchButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    filterStatusText = filterStatus([reportId]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                    // Check filters are cleared
                    await expect(receiverDropdown(dailyDataPage.page)).toHaveValue("");
                    await expect(startDate(dailyDataPage.page)).toHaveValue("");
                    await expect(endDate(dailyDataPage.page)).toHaveValue("");
                    await expect(startTime(dailyDataPage.page)).toHaveValue("");
                    await expect(endTime(dailyDataPage.page)).toHaveValue("");
                });
            });

            test.describe("table", () => {
                test("has correct headers", async ({ dailyDataPage }) => {
                    await tableHeaders(dailyDataPage.page);
                });

                test("has pagination", async ({ dailyDataPage }) => {
                    await expect(dailyDataPage.page.locator('[aria-label="Pagination"]')).toBeAttached();
                });
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test("nav contains the 'Daily Data' option", async ({ dailyDataPage }) => {
            const navItems = dailyDataPage.page.locator(".usa-nav  li");
            await expect(navItems).toContainText(["Daily Data"]);
        });

        test("has correct title", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page).toHaveTitle(dailyDataPage.title);
        });

        test("has filter", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page.getByTestId("filter-form")).toBeAttached();
        });

        test("table has correct headers", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page.locator(".usa-table th").nth(0)).toHaveText(/Report ID/);
            await expect(dailyDataPage.page.locator(".usa-table th").nth(1)).toHaveText(/Time received/);
            await expect(dailyDataPage.page.locator(".usa-table th").nth(2)).toHaveText(/File available until/);
            await expect(dailyDataPage.page.locator(".usa-table th").nth(3)).toHaveText(/Items/);
            await expect(dailyDataPage.page.locator(".usa-table th").nth(4)).toHaveText(/Filename/);
            await expect(dailyDataPage.page.locator(".usa-table th").nth(5)).toHaveText(/Receiver/);
        });

        test("table has pagination", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page.locator('[aria-label="Pagination"]')).toBeAttached();
        });

        test("has footer", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page.locator("footer")).toBeAttached();
        });

        test.describe("filter", () => {
            test.describe("onLoad", () => {
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

            test.describe("with receiver selected", () => {
                test.beforeEach(async ({ dailyDataPage }) => {
                    await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_AK_RECEIVER);
                });

                test.afterEach(async ({ dailyDataPage }) => {
                    await filterReset(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                });

                test("table loads with selected receiver data", async ({ dailyDataPage }) => {
                    await dailyDataPage.page
                        .getByRole("button", {
                            name: "Apply",
                        })
                        .click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Check that table data contains the receiver selected
                    await expectTableColumnValues(dailyDataPage.page, 5, `${TEST_ORG_AK_RECEIVER}`);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([TEST_ORG_AK_RECEIVER]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                    // Receiver dropdown persists
                    await expect(receiverDropdown(dailyDataPage.page)).toHaveValue(TEST_ORG_AK_RECEIVER);
                });

                test("with 'From' date", async ({ dailyDataPage }) => {
                    await expect(startDate(dailyDataPage.page)).toHaveValue("");

                    await setDate(dailyDataPage.page, "#start-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'To' date", async ({ dailyDataPage }) => {
                    await expect(endDate(dailyDataPage.page)).toHaveValue("");

                    await setDate(dailyDataPage.page, "#end-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'Start time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#start-date", 7);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'End time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#start-date", 7);
                    await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'Start time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#end-date", 7);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'End time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#end-date", 7);
                    await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'Start time' and 'End time'", async ({ dailyDataPage }) => {
                    // Start time
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    await startTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                    await expect(applyButton(dailyDataPage.page)).toBeEnabled();

                    // End time
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    await endTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(dailyDataPage.page)).toBeEnabled();

                    // Start time and End time
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    await startTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                    await endTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(dailyDataPage.page)).toBeEnabled();
                });

                test("with 'From' date and 'To' date", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        TEST_ORG_AK_RECEIVER,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time'", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Form values persist
                    await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                    await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                    await expect(dailyDataPage.page.locator("#start-time")).toHaveValue(defaultStartTime);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        TEST_ORG_AK_RECEIVER,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${"11:59pm"}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'End time'", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Form values persist
                    await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                    await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                    await expect(dailyDataPage.page.locator("#end-time")).toHaveValue(defaultEndTime);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        TEST_ORG_AK_RECEIVER,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${"12:00am"}–${defaultEndTime}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time', 'End time'", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        TEST_ORG_AK_RECEIVER,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${defaultEndTime}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#start-date", 0);
                    await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#start-time", defaultEndTime);
                    await setTime(dailyDataPage.page, "#end-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });
            });

            test.describe("no receiver selected", () => {
                test.beforeEach(async ({ dailyDataPage }) => {
                    await dailyDataPage.page.locator("#receiver-dropdown").selectOption("");
                });

                test.afterEach(async ({ dailyDataPage }) => {
                    await filterReset(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                });

                test("with 'From' date", async ({ dailyDataPage }) => {
                    await expect(startDate(dailyDataPage.page)).toHaveValue("");

                    await setDate(dailyDataPage.page, "#start-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'To' date", async ({ dailyDataPage }) => {
                    await expect(endDate(dailyDataPage.page)).toHaveValue("");

                    await setDate(dailyDataPage.page, "#end-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'Start time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#start-date", 7);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'End time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#start-date", 7);
                    await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'Start time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#end-date", 7);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'End time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#end-date", 7);
                    await setTime(dailyDataPage.page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });

                test("with 'Start time' and 'End time'", async ({ dailyDataPage }) => {
                    // Start time
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    await startTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                    await expect(applyButton(dailyDataPage.page)).toBeDisabled();

                    // End time
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    await endTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(dailyDataPage.page)).toBeDisabled();

                    // Start time and End time
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                    await startTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#start-time")).toHaveValue("");
                    await endTimeClear(dailyDataPage.page).click();
                    await expect(dailyDataPage.page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(dailyDataPage.page)).toBeDisabled();
                });

                test("with 'From' date and 'To' date", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.getByTestId("filter-status").waitFor({ timeout: 3000 });

                    // Form values persist
                    await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                    await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time'", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.getByTestId("filter-status").waitFor({ timeout: 3000 });

                    // Form values persist
                    await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                    await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                    await expect(dailyDataPage.page.locator("#start-time")).toHaveValue(defaultStartTime);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${"11:59pm"}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'End time'", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.getByTestId("filter-status").waitFor({ timeout: 3000 });

                    // Form values persist
                    await expect(startDate(dailyDataPage.page)).toHaveValue(fromDate);
                    await expect(endDate(dailyDataPage.page)).toHaveValue(toDate);
                    await expect(dailyDataPage.page.locator("#end-time")).toHaveValue(defaultEndTime);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${"12:00am"}–${defaultEndTime}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time', 'End time'", async ({ dailyDataPage }) => {
                    const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                    const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                    await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(dailyDataPage.page).click();
                    await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                    await dailyDataPage.page.getByTestId("filter-status").waitFor({ timeout: 3000 });

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus([
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${defaultEndTime}`,
                    ]);
                    await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({ dailyDataPage }) => {
                    await setDate(dailyDataPage.page, "#start-date", 0);
                    await setDate(dailyDataPage.page, "#end-date", 0);
                    await setTime(dailyDataPage.page, "#start-time", defaultEndTime);
                    await setTime(dailyDataPage.page, "#end-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(dailyDataPage.page)).toHaveAttribute("disabled");
                });
            });

            test("clears search on 'Apply'", async ({ dailyDataPage }) => {
                // Search by Report ID
                const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                await searchInput(dailyDataPage.page).fill(reportId);
                await searchButton(dailyDataPage.page).click();

                // Check filter status lists receiver value
                let filterStatusText = filterStatus([reportId]);
                await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                // Perform search with filters selected
                await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_AK_RECEIVER);
                const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                const toDate = await setDate(dailyDataPage.page, "#end-date", 0);

                await applyButton(dailyDataPage.page).click();
                await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                // Check filter status lists receiver value
                filterStatusText = filterStatus([
                    TEST_ORG_AK_RECEIVER,
                    `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                ]);
                await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                // Check search is cleared
                await expect(searchInput(dailyDataPage.page)).toHaveValue("");
            });

            test.describe("on reset", () => {
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
            test("returns match for Report ID", async ({ dailyDataPage }) => {
                const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                await searchInput(dailyDataPage.page).fill(reportId);
                await searchButton(dailyDataPage.page).click();

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
                await searchInput(dailyDataPage.page).fill(fileName);
                await searchButton(dailyDataPage.page).click();

                const rowCount = await tableRows(dailyDataPage.page).count();
                expect(rowCount).toEqual(1);

                // Check filter status lists receiver value
                const filterStatusText = filterStatus([fileName]);
                await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                //Check table data matches search
                expect(await tableDataCellValue(dailyDataPage.page, 0, 4)).toEqual(fileName);
            });

            test("on reset clears search results", async ({ dailyDataPage }) => {
                const fileName = await tableDataCellValue(dailyDataPage.page, 0, 4);
                await searchInput(dailyDataPage.page).fill(fileName);
                await searchButton(dailyDataPage.page).click();

                await searchReset(dailyDataPage.page).click();
                await expect(searchInput(dailyDataPage.page)).toHaveValue("");
            });

            test("clears filters on search", async ({ dailyDataPage }) => {
                // Perform search with all filters selected
                await dailyDataPage.page.locator("#receiver-dropdown").selectOption(TEST_ORG_AK_RECEIVER);
                const fromDate = await setDate(dailyDataPage.page, "#start-date", 14);
                const toDate = await setDate(dailyDataPage.page, "#end-date", 0);
                await setTime(dailyDataPage.page, "#start-time", defaultStartTime);
                await setTime(dailyDataPage.page, "#end-time", defaultEndTime);

                await applyButton(dailyDataPage.page).click();
                await dailyDataPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });

                // Check filter status lists receiver value
                let filterStatusText = filterStatus([
                    TEST_ORG_AK_RECEIVER,
                    `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    `${defaultStartTime}–${defaultEndTime}`,
                ]);
                await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                // Search by Report ID
                const reportId = await tableDataCellValue(dailyDataPage.page, 0, 0);
                await searchInput(dailyDataPage.page).fill(reportId);
                await searchButton(dailyDataPage.page).click();

                // Check filter status lists receiver value
                filterStatusText = filterStatus([reportId]);
                await expect(dailyDataPage.page.getByTestId("filter-status")).toContainText(filterStatusText);

                // Check filters are cleared
                await expect(receiverDropdown(dailyDataPage.page)).toHaveValue("");
                await expect(startDate(dailyDataPage.page)).toHaveValue("");
                await expect(endDate(dailyDataPage.page)).toHaveValue("");
                await expect(startTime(dailyDataPage.page)).toHaveValue("");
                await expect(endTime(dailyDataPage.page)).toHaveValue("");
            });
        });

        test.describe("table", () => {
            test("has correct headers", async ({ dailyDataPage }) => {
                await tableHeaders(dailyDataPage.page);
            });

            test("has pagination", async ({ dailyDataPage }) => {
                await expect(dailyDataPage.page.locator('[aria-label="Pagination"]')).toBeAttached();
            });
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test("has correct title", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page).toHaveTitle(dailyDataPage.title);
        });

        test("has footer", async ({ dailyDataPage }) => {
            await expect(dailyDataPage.page.locator("footer")).toBeAttached();
        });
    });
});
