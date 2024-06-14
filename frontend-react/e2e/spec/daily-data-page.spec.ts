import { expect, test } from "@playwright/test";

import { format } from "date-fns";
import {
    expectTableColumnValues,
    selectTestOrg,
    tableDataCellValue,
    tableRows,
    TEST_ORG_AK_RECEIVER,
    TEST_ORG_IGNORE_RECEIVER,
    waitForAPIResponse,
} from "../helpers/utils";
import * as dailyData from "../pages/daily-data";
import {
    applyButton,
    endDate,
    endTime,
    endTimeClear,
    filterReset,
    filterStatus,
    mockGetOrgAlaskaReceiversResponse,
    mockGetOrgIgnoreReceiversResponse,
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
} from "../pages/daily-data";
import {
    mockGetDeliveriesForOrgAlaskaResponse,
    mockGetDeliveriesForOrgIgnoreResponse,
} from "../pages/report-details";

const defaultStartTime = "9:00am";
const defaultEndTime = "11:30pm";

test.describe("Daily Data page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await dailyData.goto(page);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await dailyData.goto(page);
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
                await mockGetOrgIgnoreReceiversResponse(page);
                await mockGetDeliveriesForOrgIgnoreResponse(page);
                await mockGetDeliveriesForOrgIgnoreResponse(page, true);
                await mockGetDeliveriesForOrgIgnoreResponse(page, false, true);
                await mockGetDeliveriesForOrgIgnoreResponse(
                    page,
                    false,
                    false,
                    TEST_ORG_IGNORE_RECEIVER,
                );
                await dailyData.goto(page);
            });

            test("nav contains the 'Daily Data' option", async ({ page }) => {
                const navItems = page.locator(".usa-nav  li");
                await expect(navItems).toContainText(["Daily Data"]);
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveTitle(/Daily Data - ReportStream/);
            });

            test("has receiver services dropdown", async ({ page }) => {
                await expect(page.locator("#receiver-dropdown")).toBeAttached();
            });

            test("has filter", async ({ page }) => {
                await expect(page.getByTestId("filter-form")).toBeAttached();
            });

            test("table has correct headers", async ({ page }) => {
                await expect(page.locator(".usa-table th").nth(0)).toHaveText(
                    /Report ID/,
                );
                await expect(page.locator(".usa-table th").nth(1)).toHaveText(
                    /Time received/,
                );
                await expect(page.locator(".usa-table th").nth(2)).toHaveText(
                    /File available until/,
                );
                await expect(page.locator(".usa-table th").nth(3)).toHaveText(
                    /Items/,
                );
                await expect(page.locator(".usa-table th").nth(4)).toHaveText(
                    /Filename/,
                );
                await expect(page.locator(".usa-table th").nth(5)).toHaveText(
                    /Receiver/,
                );
            });

            test("table has pagination", async ({ page }) => {
                await expect(page.getByTestId("Pagination")).toBeAttached();
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });

            test.describe("filter", () => {
                test.describe("onLoad", () => {
                    test("does not have a receiver selected", async ({
                        page,
                    }) => {
                        await expect(receiverDropdown(page)).toBeAttached();
                        await expect(receiverDropdown(page)).toHaveValue("");
                    });

                    test("'From' date does not have a value", async ({
                        page,
                    }) => {
                        await expect(startDate(page)).toBeAttached();
                        await expect(startDate(page)).toHaveValue("");
                    });

                    test("'To' date does not have a value", async ({
                        page,
                    }) => {
                        await expect(endDate(page)).toBeAttached();
                        await expect(endDate(page)).toHaveValue("");
                    });

                    test("'Start time' does not have a value", async ({
                        page,
                    }) => {
                        await expect(startTime(page)).toBeAttached();
                        await expect(startTime(page)).toHaveText("");
                    });

                    test("'End time'' does not have a value", async ({
                        page,
                    }) => {
                        await expect(endTime(page)).toBeAttached();
                        await expect(endTime(page)).toHaveText("");
                    });
                });

                test.describe("with receiver selected", () => {
                    test.beforeEach(async ({ page }) => {
                        await page
                            .locator("#receiver-dropdown")
                            .selectOption(TEST_ORG_IGNORE_RECEIVER);
                    });

                    test.afterEach(async ({ page }) => {
                        await filterReset(page).click();
                    });

                    test.skip("table loads with selected receiver data", async ({
                        page,
                    }) => {
                        await page
                            .getByRole("button", {
                                name: "Apply",
                            })
                            .click();

                        // Check that table data contains the receiver selected
                        await expectTableColumnValues(
                            page,
                            5,
                            `${TEST_ORG_IGNORE_RECEIVER}`,
                        );

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus(page, [
                            TEST_ORG_IGNORE_RECEIVER,
                        ]);
                        await expect(
                            page.getByTestId("filter-status"),
                        ).toContainText(filterStatusText);

                        // Receiver dropdown persists
                        await expect(receiverDropdown(page)).toHaveValue(
                            TEST_ORG_IGNORE_RECEIVER,
                        );
                    });

                    test("with 'From' date", async ({ page }) => {
                        await expect(startDate(page)).toHaveValue("");

                        await setDate(page, "#start-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'To' date", async ({ page }) => {
                        await expect(endDate(page)).toHaveValue("");

                        await setDate(page, "#end-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'From' date and 'Start time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#start-date", 7);
                        await setTime(page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'From' date and 'End time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#start-date", 7);
                        await setTime(page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'To' date and 'Start time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#end-date", 7);
                        await setTime(page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'To' date and 'End time'", async ({ page }) => {
                        await setDate(page, "#end-date", 7);
                        await setTime(page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'Start time' and 'End time'", async ({
                        page,
                    }) => {
                        // Start time
                        await setTime(page, "#start-time", defaultStartTime);
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                        await startTimeClear(page).click();
                        await expect(page.locator("#start-time")).toHaveValue(
                            "",
                        );
                        await expect(applyButton(page)).toBeEnabled();

                        // End time
                        await setTime(page, "#end-time", defaultEndTime);
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                        await endTimeClear(page).click();
                        await expect(page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(page)).toBeEnabled();

                        // Start time and End time
                        await setTime(page, "#start-time", defaultStartTime);
                        await setTime(page, "#end-time", defaultEndTime);
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                        await startTimeClear(page).click();
                        await expect(page.locator("#start-time")).toHaveValue(
                            "",
                        );
                        await endTimeClear(page).click();
                        await expect(page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(page)).toBeEnabled();
                    });

                    test.skip("with 'From' date and 'To' date", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus(page, [
                            TEST_ORG_IGNORE_RECEIVER,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        ]);
                        await expect(
                            page.getByTestId("filter-status"),
                        ).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'Start time'", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);
                        await setTime(page, "#start-time", defaultStartTime);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(page)).toHaveValue(fromDate);
                        await expect(endDate(page)).toHaveValue(toDate);
                        await expect(page.locator("#start-time")).toHaveValue(
                            defaultStartTime,
                        );

                        // TODO: uncomment code to use with live data
                        // Check filter status lists receiver value
                        // const filterStatusText = filterStatus(page, [
                        //     TEST_ORG_IGNORE_RECEIVER,
                        //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        //     `${defaultStartTime}–${"11:59pm"}`,
                        // ]);
                        // await expect(
                        //     page.getByTestId("filter-status"),
                        // ).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'End time'", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);
                        await setTime(page, "#end-time", defaultEndTime);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(page)).toHaveValue(fromDate);
                        await expect(endDate(page)).toHaveValue(toDate);
                        await expect(page.locator("#end-time")).toHaveValue(
                            defaultEndTime,
                        );

                        // TODO: uncomment code to use with live data
                        // Check filter status lists receiver value
                        // const filterStatusText = filterStatus(page, [
                        //     TEST_ORG_IGNORE_RECEIVER,
                        //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        //     `${"12:00am"}–${defaultEndTime}`,
                        // ]);
                        // await expect(
                        //     page.getByTestId("filter-status"),
                        // ).toContainText(filterStatusText);
                    });

                    test.skip("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);
                        await setTime(page, "#start-time", defaultStartTime);
                        await setTime(page, "#end-time", defaultEndTime);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // TODO: uncomment code to use with live data
                        // Check that table data contains the dates/times that were selected
                        // const areDatesInRange =
                        //     await tableColumnDateTimeInRange(
                        //         page,
                        //         1,
                        //         fromDate,
                        //         toDate,
                        //         defaultStartTime,
                        //         defaultEndTime,
                        //     );
                        // expect(areDatesInRange).toBe(true);

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus(page, [
                            TEST_ORG_IGNORE_RECEIVER,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${defaultStartTime}–${defaultEndTime}`,
                        ]);
                        await expect(
                            page.getByTestId("filter-status"),
                        ).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#start-date", 0);
                        await setDate(page, "#end-date", 0);
                        await setTime(page, "#start-time", defaultEndTime);
                        await setTime(page, "#end-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });
                });

                test.describe("no receiver selected", () => {
                    test.beforeEach(async ({ page }) => {
                        await page
                            .locator("#receiver-dropdown")
                            .selectOption("");
                    });

                    test.afterEach(async ({ page }) => {
                        await filterReset(page).click();
                    });

                    test("with 'From' date", async ({ page }) => {
                        await expect(startDate(page)).toHaveValue("");

                        await setDate(page, "#start-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'To' date", async ({ page }) => {
                        await expect(endDate(page)).toHaveValue("");

                        await setDate(page, "#end-date", 7);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'From' date and 'Start time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#start-date", 7);
                        await setTime(page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'From' date and 'End time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#start-date", 7);
                        await setTime(page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'To' date and 'Start time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#end-date", 7);
                        await setTime(page, "#start-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'To' date and 'End time'", async ({ page }) => {
                        await setDate(page, "#end-date", 7);
                        await setTime(page, "#end-time", "8:00pm");

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });

                    test("with 'Start time' and 'End time'", async ({
                        page,
                    }) => {
                        // Start time
                        await setTime(page, "#start-time", defaultStartTime);
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                        await startTimeClear(page).click();
                        await expect(page.locator("#start-time")).toHaveValue(
                            "",
                        );
                        await expect(applyButton(page)).toBeDisabled();

                        // End time
                        await setTime(page, "#end-time", defaultEndTime);
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                        await endTimeClear(page).click();
                        await expect(page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(page)).toBeDisabled();

                        // Start time and End time
                        await setTime(page, "#start-time", defaultStartTime);
                        await setTime(page, "#end-time", defaultEndTime);
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                        await startTimeClear(page).click();
                        await expect(page.locator("#start-time")).toHaveValue(
                            "",
                        );
                        await endTimeClear(page).click();
                        await expect(page.locator("#end-time")).toHaveValue("");
                        await expect(applyButton(page)).toBeDisabled();
                    });

                    test.skip("with 'From' date and 'To' date", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus(page, [
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        ]);
                        await expect(
                            page.getByTestId("filter-status"),
                        ).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'Start time'", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);
                        await setTime(page, "#start-time", defaultStartTime);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(page)).toHaveValue(fromDate);
                        await expect(endDate(page)).toHaveValue(toDate);
                        await expect(page.locator("#start-time")).toHaveValue(
                            defaultStartTime,
                        );

                        // TODO: uncomment code to use with live data
                        // Check filter status lists receiver value
                        // const filterStatusText = filterStatus(page, [
                        //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        //     `${defaultStartTime}–${"11:59pm"}`,
                        // ]);
                        // await expect(
                        //     page.getByTestId("filter-status"),
                        // ).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'End time'", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);
                        await setTime(page, "#end-time", defaultEndTime);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Form values persist
                        await expect(startDate(page)).toHaveValue(fromDate);
                        await expect(endDate(page)).toHaveValue(toDate);
                        await expect(page.locator("#end-time")).toHaveValue(
                            defaultEndTime,
                        );

                        // TODO: uncomment code to use with live data
                        // Check filter status lists receiver value
                        // const filterStatusText = filterStatus(page, [
                        //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        //     `${"12:00am"}–${defaultEndTime}`,
                        // ]);
                        // await expect(
                        //     page.getByTestId("filter-status"),
                        // ).toContainText(filterStatusText);
                    });

                    test.skip("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
                        page,
                    }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);
                        await setTime(page, "#start-time", defaultStartTime);
                        await setTime(page, "#end-time", defaultEndTime);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // TODO: uncomment code to use with live data
                        // Check that table data contains the dates/times that were selected
                        // const areDatesInRange =
                        //     await tableColumnDateTimeInRange(
                        //         page,
                        //         1,
                        //         fromDate,
                        //         toDate,
                        //         defaultStartTime,
                        //         defaultEndTime,
                        //     );
                        // expect(areDatesInRange).toBe(true);

                        // Check filter status lists receiver value
                        const filterStatusText = filterStatus(page, [
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${defaultStartTime}–${defaultEndTime}`,
                        ]);
                        await expect(
                            page.getByTestId("filter-status"),
                        ).toContainText(filterStatusText);
                    });

                    test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({
                        page,
                    }) => {
                        await setDate(page, "#start-date", 0);
                        await setDate(page, "#end-date", 0);
                        await setTime(page, "#start-time", defaultEndTime);
                        await setTime(page, "#end-time", defaultStartTime);

                        // Apply button is disabled
                        await expect(applyButton(page)).toHaveAttribute(
                            "disabled",
                        );
                    });
                });

                test.describe("on reset", () => {
                    test("form elements clear", async ({ page }) => {
                        await filterReset(page).click();
                        await expect(receiverDropdown(page)).toHaveValue("");
                        await expect(startDate(page)).toHaveValue("");
                        await expect(endDate(page)).toHaveValue("");
                        await expect(startTime(page)).toHaveValue("");
                        await expect(endTime(page)).toHaveValue("");
                    });
                });

                test("clears search on 'Apply'", async ({ page }) => {
                    // Search by Report ID
                    const reportId = await tableDataCellValue(page, 0, 0);
                    await searchInput(page).fill(reportId);
                    await searchButton(page).click();

                    // TODO: uncomment code to use with live data
                    // const rowCount = await tableRows(page).count();
                    // expect(rowCount).toEqual(1);

                    // Check filter status lists receiver value
                    // let filterStatusText = filterStatus(page, [reportId]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);

                    // Perform search with filters selected
                    await page
                        .locator("#receiver-dropdown")
                        .selectOption(TEST_ORG_IGNORE_RECEIVER);
                    // const fromDate = await setDate(page, "#start-date", 14);
                    // const toDate = await setDate(page, "#end-date", 0);
                    await setDate(page, "#start-date", 14);
                    await setDate(page, "#end-date", 0);

                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    // filterStatusText = filterStatus(page, [
                    //     TEST_ORG_IGNORE_RECEIVER,
                    //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    // ]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);

                    // Check search is cleared
                    await expect(searchInput(page)).toHaveValue("");
                });
            });

            test.describe("search", () => {
                test.skip("returns match for Report ID", async ({ page }) => {
                    const reportId = await tableDataCellValue(page, 0, 0);
                    await searchInput(page).fill(reportId);
                    await searchButton(page).click();

                    const rowCount = await tableRows(page).count();
                    expect(rowCount).toEqual(1);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus(page, [reportId]);
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(filterStatusText);

                    //Check table data matches search
                    expect(await tableDataCellValue(page, 0, 0)).toEqual(
                        reportId,
                    );
                });

                test.skip("returns match for Filename", async ({ page }) => {
                    const fileName = await tableDataCellValue(page, 2, 4);
                    await searchInput(page).fill(fileName);
                    await searchButton(page).click();

                    const rowCount = await tableRows(page).count();
                    expect(rowCount).toEqual(1);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus(page, [fileName]);
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(filterStatusText);

                    //Check table data matches search
                    expect(await tableDataCellValue(page, 0, 4)).toEqual(
                        fileName,
                    );
                });

                test("on reset clears search results", async ({ page }) => {
                    const fileName = await tableDataCellValue(page, 1, 4);
                    await searchInput(page).fill(fileName);
                    await searchButton(page).click();

                    await searchReset(page).click();
                    await expect(searchInput(page)).toHaveValue("");
                });

                test("clears filters on search", async ({ page }) => {
                    // TODO: uncomment code to use with live data
                    // Perform search with all filters selected
                    await page
                        .locator("#receiver-dropdown")
                        .selectOption(TEST_ORG_IGNORE_RECEIVER);
                    // const fromDate = await setDate(page, "#start-date", 14);
                    // const toDate = await setDate(page, "#end-date", 0);
                    await setDate(page, "#start-date", 14);
                    await setDate(page, "#end-date", 0);
                    await setTime(page, "#start-time", defaultStartTime);
                    await setTime(page, "#end-time", defaultEndTime);

                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    // let filterStatusText = filterStatus(page, [
                    //     TEST_ORG_IGNORE_RECEIVER,
                    //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    //     `${defaultStartTime}–${defaultEndTime}`,
                    // ]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);

                    const reportId = "729158ce-4125-46fa-bea0-3c0f910f472c";
                    await searchInput(page).fill(reportId);
                    await searchButton(page).click();

                    // Check filter status lists receiver value
                    // filterStatusText = filterStatus(page, [reportId]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);

                    //Check table data matches search
                    // expect(await tableDataCellValue(page, 0, 0)).toEqual(
                    //     reportId,
                    // );

                    // Check filters are cleared
                    await expect(receiverDropdown(page)).toHaveValue("");
                    await expect(startDate(page)).toHaveValue("");
                    await expect(endDate(page)).toHaveValue("");
                    await expect(startTime(page)).toHaveValue("");
                    await expect(endTime(page)).toHaveValue("");
                });
            });

            test.describe("table", () => {
                test("has correct headers", async ({ page }) => {
                    await tableHeaders(page);
                });

                test("has pagination", async ({ page }) => {
                    await expect(page.getByTestId("Pagination")).toBeAttached();
                });
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await mockGetOrgAlaskaReceiversResponse(page);
            await mockGetDeliveriesForOrgAlaskaResponse(page);
            await mockGetDeliveriesForOrgAlaskaResponse(page, true);
            await mockGetDeliveriesForOrgAlaskaResponse(page, false, true);
            await mockGetDeliveriesForOrgAlaskaResponse(
                page,
                false,
                false,
                TEST_ORG_AK_RECEIVER,
            );
            await dailyData.goto(page);
            await page.getByTestId("filter-form").waitFor({ state: "visible" });
        });

        test("nav contains the 'Daily Data' option", async ({ page }) => {
            const navItems = page.locator(".usa-nav  li");
            await expect(navItems).toContainText(["Daily Data"]);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Daily Data - ReportStream/);
        });

        test("has filter", async ({ page }) => {
            await expect(page.getByTestId("filter-form")).toBeAttached();
        });

        test("table has correct headers", async ({ page }) => {
            await expect(page.locator(".usa-table th").nth(0)).toHaveText(
                /Report ID/,
            );
            await expect(page.locator(".usa-table th").nth(1)).toHaveText(
                /Time received/,
            );
            await expect(page.locator(".usa-table th").nth(2)).toHaveText(
                /File available until/,
            );
            await expect(page.locator(".usa-table th").nth(3)).toHaveText(
                /Items/,
            );
            await expect(page.locator(".usa-table th").nth(4)).toHaveText(
                /Filename/,
            );
            await expect(page.locator(".usa-table th").nth(5)).toHaveText(
                /Receiver/,
            );
        });

        test("table has pagination", async ({ page }) => {
            await expect(page.getByTestId("Pagination")).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });

        test.describe("filter", () => {
            test.describe("onLoad", () => {
                test("does not have a receiver selected", async ({ page }) => {
                    await expect(receiverDropdown(page)).toBeAttached();
                    await expect(receiverDropdown(page)).toHaveValue("");
                });

                test("'From' date does not have a value", async ({ page }) => {
                    await expect(startDate(page)).toBeAttached();
                    await expect(startDate(page)).toHaveValue("");
                });

                test("'To' date does not have a value", async ({ page }) => {
                    await expect(endDate(page)).toBeAttached();
                    await expect(endDate(page)).toHaveValue("");
                });

                test("'Start time' does not have a value", async ({ page }) => {
                    await expect(startTime(page)).toBeAttached();
                    await expect(startTime(page)).toHaveText("");
                });

                test("'End time'' does not have a value", async ({ page }) => {
                    await expect(endTime(page)).toBeAttached();
                    await expect(endTime(page)).toHaveText("");
                });
            });

            test.describe("with receiver selected", () => {
                test.beforeEach(async ({ page }) => {
                    await page
                        .locator("#receiver-dropdown")
                        .selectOption(TEST_ORG_AK_RECEIVER);
                });

                test.afterEach(async ({ page }) => {
                    await filterReset(page).click();
                });

                test("table loads with selected receiver data", async ({
                    page,
                }) => {
                    await page
                        .getByRole("button", {
                            name: "Apply",
                        })
                        .click();

                    // Check that table data contains the receiver selected
                    await expectTableColumnValues(
                        page,
                        5,
                        `${TEST_ORG_AK_RECEIVER}`,
                    );

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus(page, [
                        TEST_ORG_AK_RECEIVER,
                    ]);
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(filterStatusText);

                    // Receiver dropdown persists
                    await expect(receiverDropdown(page)).toHaveValue(
                        TEST_ORG_AK_RECEIVER,
                    );
                });

                test("with 'From' date", async ({ page }) => {
                    await expect(startDate(page)).toHaveValue("");

                    await setDate(page, "#start-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'To' date", async ({ page }) => {
                    await expect(endDate(page)).toHaveValue("");

                    await setDate(page, "#end-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'Start time'", async ({ page }) => {
                    await setDate(page, "#start-date", 7);
                    await setTime(page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'End time'", async ({ page }) => {
                    await setDate(page, "#start-date", 7);
                    await setTime(page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'Start time'", async ({ page }) => {
                    await setDate(page, "#end-date", 7);
                    await setTime(page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'End time'", async ({ page }) => {
                    await setDate(page, "#end-date", 7);
                    await setTime(page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'Start time' and 'End time'", async ({ page }) => {
                    // Start time
                    await setTime(page, "#start-time", defaultStartTime);
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                    await startTimeClear(page).click();
                    await expect(page.locator("#start-time")).toHaveValue("");
                    await expect(applyButton(page)).toBeEnabled();

                    // End time
                    await setTime(page, "#end-time", defaultEndTime);
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                    await endTimeClear(page).click();
                    await expect(page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(page)).toBeEnabled();

                    // Start time and End time
                    await setTime(page, "#start-time", defaultStartTime);
                    await setTime(page, "#end-time", defaultEndTime);
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                    await startTimeClear(page).click();
                    await expect(page.locator("#start-time")).toHaveValue("");
                    await endTimeClear(page).click();
                    await expect(page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(page)).toBeEnabled();
                });

                test.skip("with 'From' date and 'To' date", async ({
                    page,
                }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus(page, [
                        TEST_ORG_AK_RECEIVER,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    ]);
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time'", async ({
                    page,
                }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);
                    await setTime(page, "#start-time", defaultStartTime);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Form values persist
                    await expect(startDate(page)).toHaveValue(fromDate);
                    await expect(endDate(page)).toHaveValue(toDate);
                    await expect(page.locator("#start-time")).toHaveValue(
                        defaultStartTime,
                    );

                    // Check filter status lists receiver value
                    // const filterStatusText = filterStatus(page, [
                    //     TEST_ORG_AK_RECEIVER,
                    //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    //     `${defaultStartTime}–${"11:59pm"}`,
                    // ]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'End time'", async ({
                    page,
                }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);
                    await setTime(page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Form values persist
                    await expect(startDate(page)).toHaveValue(fromDate);
                    await expect(endDate(page)).toHaveValue(toDate);
                    await expect(page.locator("#end-time")).toHaveValue(
                        defaultEndTime,
                    );

                    // Check filter status lists receiver value
                    // const filterStatusText = filterStatus(page, [
                    //     TEST_ORG_AK_RECEIVER,
                    //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    //     `${"12:00am"}–${defaultEndTime}`,
                    // ]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);
                });

                test.skip("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
                    page,
                }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);
                    await setTime(page, "#start-time", defaultStartTime);
                    await setTime(page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Only needed when using live data
                    // Check that table data contains the dates/times that were selected
                    // const areDatesInRange = await tableColumnDateTimeInRange(
                    //     page,
                    //     1,
                    //     fromDate,
                    //     toDate,
                    //     defaultStartTime,
                    //     defaultEndTime,
                    // );
                    // expect(areDatesInRange).toBe(true);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus(page, [
                        TEST_ORG_AK_RECEIVER,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${defaultEndTime}`,
                    ]);
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({
                    page,
                }) => {
                    await setDate(page, "#start-date", 0);
                    await setDate(page, "#end-date", 0);
                    await setTime(page, "#start-time", defaultEndTime);
                    await setTime(page, "#end-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });
            });

            test.describe("no receiver selected", () => {
                test.beforeEach(async ({ page }) => {
                    await page.locator("#receiver-dropdown").selectOption("");
                });

                test.afterEach(async ({ page }) => {
                    await filterReset(page).click();
                });

                test("with 'From' date", async ({ page }) => {
                    await expect(startDate(page)).toHaveValue("");

                    await setDate(page, "#start-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'To' date", async ({ page }) => {
                    await expect(endDate(page)).toHaveValue("");

                    await setDate(page, "#end-date", 7);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'Start time'", async ({ page }) => {
                    await setDate(page, "#start-date", 7);
                    await setTime(page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'End time'", async ({ page }) => {
                    await setDate(page, "#start-date", 7);
                    await setTime(page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'Start time'", async ({ page }) => {
                    await setDate(page, "#end-date", 7);
                    await setTime(page, "#start-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'End time'", async ({ page }) => {
                    await setDate(page, "#end-date", 7);
                    await setTime(page, "#end-time", "8:00pm");

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });

                test("with 'Start time' and 'End time'", async ({ page }) => {
                    // Start time
                    await setTime(page, "#start-time", defaultStartTime);
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                    await startTimeClear(page).click();
                    await expect(page.locator("#start-time")).toHaveValue("");
                    await expect(applyButton(page)).toBeDisabled();

                    // End time
                    await setTime(page, "#end-time", defaultEndTime);
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                    await endTimeClear(page).click();
                    await expect(page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(page)).toBeDisabled();

                    // Start time and End time
                    await setTime(page, "#start-time", defaultStartTime);
                    await setTime(page, "#end-time", defaultEndTime);
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                    await startTimeClear(page).click();
                    await expect(page.locator("#start-time")).toHaveValue("");
                    await endTimeClear(page).click();
                    await expect(page.locator("#end-time")).toHaveValue("");
                    await expect(applyButton(page)).toBeDisabled();
                });

                test("with 'From' date and 'To' date", async ({ page }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .getByTestId("filter-status")
                        .waitFor({ timeout: 3000 });

                    // Form values persist
                    await expect(startDate(page)).toHaveValue(fromDate);
                    await expect(endDate(page)).toHaveValue(toDate);

                    // Check filter status lists receiver value
                    // const filterStatusText = filterStatus(page, [
                    //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    // ]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time'", async ({
                    page,
                }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);
                    await setTime(page, "#start-time", defaultStartTime);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .getByTestId("filter-status")
                        .waitFor({ timeout: 3000 });

                    // Form values persist
                    await expect(startDate(page)).toHaveValue(fromDate);
                    await expect(endDate(page)).toHaveValue(toDate);
                    await expect(page.locator("#start-time")).toHaveValue(
                        defaultStartTime,
                    );

                    // Check filter status lists receiver value
                    // const filterStatusText = filterStatus(page, [
                    //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    //     `${defaultStartTime}–${"11:59pm"}`,
                    // ]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'End time'", async ({
                    page,
                }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);
                    await setTime(page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .getByTestId("filter-status")
                        .waitFor({ timeout: 3000 });

                    // Form values persist
                    await expect(startDate(page)).toHaveValue(fromDate);
                    await expect(endDate(page)).toHaveValue(toDate);
                    await expect(page.locator("#end-time")).toHaveValue(
                        defaultEndTime,
                    );

                    // Check filter status lists receiver value
                    // const filterStatusText = filterStatus(page, [
                    //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    //     `${"12:00am"}–${defaultEndTime}`,
                    // ]);
                    // await expect(
                    //     page.getByTestId("filter-status"),
                    // ).toContainText(filterStatusText);
                });

                test.skip("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
                    page,
                }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);
                    await setTime(page, "#start-time", defaultStartTime);
                    await setTime(page, "#end-time", defaultEndTime);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });
                    await page
                        .getByTestId("filter-status")
                        .waitFor({ timeout: 3000 });

                    // Only needed when using live data
                    // Check that table data contains the dates/times that were selected
                    // const areDatesInRange = await tableColumnDateTimeInRange(
                    //     page,
                    //     1,
                    //     fromDate,
                    //     toDate,
                    //     defaultStartTime,
                    //     defaultEndTime,
                    // );
                    // expect(areDatesInRange).toBe(true);

                    // Check filter status lists receiver value
                    const filterStatusText = filterStatus(page, [
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${defaultEndTime}`,
                    ]);
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(filterStatusText);
                });

                test("with 'From' date, 'To' date, 'Start time' before 'End time'", async ({
                    page,
                }) => {
                    await setDate(page, "#start-date", 0);
                    await setDate(page, "#end-date", 0);
                    await setTime(page, "#start-time", defaultEndTime);
                    await setTime(page, "#end-time", defaultStartTime);

                    // Apply button is disabled
                    await expect(applyButton(page)).toHaveAttribute("disabled");
                });
            });

            test("clears search on 'Apply'", async ({ page }) => {
                // Search by Report ID
                const reportId = await tableDataCellValue(page, 0, 0);
                await searchInput(page).fill(reportId);
                await searchButton(page).click();

                // TODO: uncomment code to use with live data
                // const rowCount = await tableRows(page).count();
                // expect(rowCount).toEqual(1);

                // Check filter status lists receiver value
                // const filterStatusText = filterStatus(page, [reportId]);
                // await expect(page.getByTestId("filter-status")).toContainText(
                //     filterStatusText,
                // );

                // Perform search with filters selected
                await page
                    .locator("#receiver-dropdown")
                    .selectOption(TEST_ORG_AK_RECEIVER);
                // const fromDate = await setDate(page, "#start-date", 14);
                // const toDate = await setDate(page, "#end-date", 0);
                await setDate(page, "#start-date", 14);
                await setDate(page, "#end-date", 0);

                await applyButton(page).click();
                await page
                    .locator(".usa-table tbody")
                    .waitFor({ state: "visible" });

                // Check filter status lists receiver value
                // filterStatusText = filterStatus(page, [
                //     TEST_ORG_AK_RECEIVER,
                //     `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                // ]);
                // await expect(page.getByTestId("filter-status")).toContainText(
                //     filterStatusText,
                // );

                // Check search is cleared
                await expect(searchInput(page)).toHaveValue("");
            });

            test.describe("on reset", () => {
                test("form elements clear", async ({ page }) => {
                    await filterReset(page).click();
                    await expect(receiverDropdown(page)).toHaveValue("");
                    await expect(startDate(page)).toHaveValue("");
                    await expect(endDate(page)).toHaveValue("");
                    await expect(startTime(page)).toHaveValue("");
                    await expect(endTime(page)).toHaveValue("");
                });
            });
        });

        test.describe("search", () => {
            test.skip("returns match for Report ID", async ({ page }) => {
                const reportId = await tableDataCellValue(page, 0, 0);
                await searchInput(page).fill(reportId);
                await searchButton(page).click();

                const rowCount = await tableRows(page).count();
                expect(rowCount).toEqual(1);

                // Check filter status lists receiver value
                const filterStatusText = filterStatus(page, [reportId]);
                await expect(page.getByTestId("filter-status")).toContainText(
                    filterStatusText,
                );

                //Check table data matches search
                expect(await tableDataCellValue(page, 0, 0)).toEqual(reportId);
            });

            test.skip("returns match for Filename", async ({ page }) => {
                const fileName = await tableDataCellValue(page, 0, 4);
                await searchInput(page).fill(fileName);
                await searchButton(page).click();

                const rowCount = await tableRows(page).count();
                expect(rowCount).toEqual(1);

                // Check filter status lists receiver value
                const filterStatusText = filterStatus(page, [fileName]);
                await expect(page.getByTestId("filter-status")).toContainText(
                    filterStatusText,
                );

                //Check table data matches search
                expect(await tableDataCellValue(page, 0, 4)).toEqual(fileName);
            });

            test("on reset clears search results", async ({ page }) => {
                const fileName = await tableDataCellValue(page, 0, 4);
                await searchInput(page).fill(fileName);
                await searchButton(page).click();

                await searchReset(page).click();
                await expect(searchInput(page)).toHaveValue("");
            });

            test("clears filters on search", async ({ page }) => {
                // Perform search with all filters selected
                await page
                    .locator("#receiver-dropdown")
                    .selectOption(TEST_ORG_AK_RECEIVER);
                const fromDate = await setDate(page, "#start-date", 14);
                const toDate = await setDate(page, "#end-date", 0);
                await setTime(page, "#start-time", defaultStartTime);
                await setTime(page, "#end-time", defaultEndTime);

                await applyButton(page).click();
                await page
                    .locator(".usa-table tbody")
                    .waitFor({ state: "visible" });

                // Check filter status lists receiver value
                let filterStatusText = filterStatus(page, [
                    TEST_ORG_AK_RECEIVER,
                    `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    `${defaultStartTime}–${defaultEndTime}`,
                ]);
                await expect(page.getByTestId("filter-status")).toContainText(
                    filterStatusText,
                );

                // Search by Report ID
                const reportId = await tableDataCellValue(page, 0, 0);
                await searchInput(page).fill(reportId);
                await searchButton(page).click();

                // Check filter status lists receiver value
                filterStatusText = filterStatus(page, [reportId]);
                await expect(page.getByTestId("filter-status")).toContainText(
                    filterStatusText,
                );

                // Check filters are cleared
                await expect(receiverDropdown(page)).toHaveValue("");
                await expect(startDate(page)).toHaveValue("");
                await expect(endDate(page)).toHaveValue("");
                await expect(startTime(page)).toHaveValue("");
                await expect(endTime(page)).toHaveValue("");
            });
        });

        test.describe("table", () => {
            test("has correct headers", async ({ page }) => {
                await tableHeaders(page);
            });

            test("has pagination", async ({ page }) => {
                await expect(page.getByTestId("Pagination")).toBeAttached();
            });
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await dailyData.goto(page);
            const response = await waitForAPIResponse(page, "/api/waters/org/");
            expect(response).toBe(200);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Daily Data - ReportStream/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });
});
