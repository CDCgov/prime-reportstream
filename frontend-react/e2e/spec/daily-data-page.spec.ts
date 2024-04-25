import { expect, test } from "@playwright/test";

import { format } from "date-fns";
import {
    expectTableColumnDateTimeInRange,
    expectTableColumnValues,
    selectTestOrg,
    waitForAPIResponse,
} from "../helpers/utils";
import * as dailyData from "../pages/daily-data";
import {
    applyButton,
    endDate,
    endTime,
    endTimeClear,
    filterStatus,
    receiverDropdown,
    resetButton,
    setDate,
    setTime,
    startDate,
    startTime,
    startTimeClear,
    tableHeaders,
} from "../pages/daily-data";

const ignoreReceiver = "FULL_ELR";
const alaskaReceiver = "elr";
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
                await dailyData.goto(page);
                const response = await waitForAPIResponse(
                    page,
                    "/api/waters/org/",
                );
                expect(response).toBe(200);
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveTitle(/Daily Data - ReportStream/);
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
                            .selectOption(ignoreReceiver);
                    });

                    test.afterEach(async ({ page }) => {
                        await resetButton(page).click();
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
                            `ignore.${ignoreReceiver}`,
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [ignoreReceiver]);

                        // Receiver dropdown persists
                        await expect(receiverDropdown(page)).toHaveValue(
                            ignoreReceiver,
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

                    test("with 'From' date and 'To' date", async ({ page }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Check that table data contains the dates that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            "",
                            "",
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            ignoreReceiver,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        ]);
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

                        // Check that table data contains the dates/times that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            defaultStartTime,
                            "",
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            ignoreReceiver,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${defaultStartTime}–${"11:59pm"}`,
                        ]);
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

                        // Check that table data contains the dates/times that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            "",
                            defaultEndTime,
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            ignoreReceiver,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${"12:00am"}–${defaultEndTime}`,
                        ]);
                    });

                    test("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
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

                        // Check that table data contains the dates/times that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            defaultStartTime,
                            defaultEndTime,
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            ignoreReceiver,
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${defaultStartTime}–${defaultEndTime}`,
                        ]);
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
                        await resetButton(page).click();
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

                    test("with 'From' date and 'To' date", async ({ page }) => {
                        const fromDate = await setDate(page, "#start-date", 14);
                        const toDate = await setDate(page, "#end-date", 0);

                        // Apply button is enabled
                        await applyButton(page).click();
                        await page
                            .locator(".usa-table tbody")
                            .waitFor({ state: "visible" });

                        // Check that table data contains the dates that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            "",
                            "",
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        ]);
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

                        // Check that table data contains the dates/times that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            defaultStartTime,
                            "",
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${defaultStartTime}–${"11:59pm"}`,
                        ]);
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

                        // Check that table data contains the dates/times that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            "",
                            defaultEndTime,
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${"12:00am"}–${defaultEndTime}`,
                        ]);
                    });

                    test("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
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

                        // Check that table data contains the dates/times that were selected
                        await expectTableColumnDateTimeInRange(
                            page,
                            1,
                            fromDate,
                            toDate,
                            defaultStartTime,
                            defaultEndTime,
                        );

                        // Check filter status lists receiver value
                        await filterStatus(page, [
                            `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                            `${defaultStartTime}–${defaultEndTime}`,
                        ]);
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
                        await resetButton(page).click();
                        await expect(receiverDropdown(page)).toHaveValue("");
                        await expect(startDate(page)).toHaveValue("");
                        await expect(endDate(page)).toHaveValue("");
                        await expect(startTime(page)).toHaveValue("");
                        await expect(endTime(page)).toHaveValue("");
                    });
                });
            });

            test.describe("table", () => {
                test("has correct headers", async ({ page }) => {
                    await tableHeaders(page);
                });

                test("has pagination", async ({ page }) => {
                    await expect(
                        page.getByTestId("Deliveries pagination"),
                    ).toBeAttached();
                });
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            // If we decide to use mock data
            // await mockGetDeliveriesForOrgResponse(
            //     page,
            //     `ak-phd.${selectedReceiver}`,
            //     "ak-phd",
            //     "AK",
            // );
            await dailyData.goto(page);
            await page.getByTestId("filter-form").waitFor({ state: "visible" });
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Daily Data - ReportStream/);
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
                        .selectOption(alaskaReceiver);
                });

                test.afterEach(async ({ page }) => {
                    await resetButton(page).click();
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
                        `ak-phd.${alaskaReceiver}`,
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [alaskaReceiver]);

                    // Receiver dropdown persists
                    await expect(receiverDropdown(page)).toHaveValue(
                        alaskaReceiver,
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

                test("with 'From' date and 'To' date", async ({ page }) => {
                    const fromDate = await setDate(page, "#start-date", 14);
                    const toDate = await setDate(page, "#end-date", 0);

                    // Apply button is enabled
                    await applyButton(page).click();
                    await page
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Check that table data contains the dates that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        "",
                        "",
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        alaskaReceiver,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    ]);
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

                    // Check that table data contains the dates/times that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        defaultStartTime,
                        "",
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        alaskaReceiver,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${"11:59pm"}`,
                    ]);
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

                    // Check that table data contains the dates/times that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        "",
                        defaultEndTime,
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        alaskaReceiver,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${"12:00am"}–${defaultEndTime}`,
                    ]);
                });

                test("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
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

                    // Check that table data contains the dates/times that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        defaultStartTime,
                        defaultEndTime,
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        alaskaReceiver,
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${defaultEndTime}`,
                    ]);
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
                    await resetButton(page).click();
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
                        .locator(".usa-table tbody")
                        .waitFor({ state: "visible" });

                    // Check that table data contains the dates that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        "",
                        "",
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                    ]);
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

                    // Check that table data contains the dates/times that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        defaultStartTime,
                        "",
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${"11:59pm"}`,
                    ]);
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

                    // Check that table data contains the dates/times that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        "",
                        defaultEndTime,
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${"12:00am"}–${defaultEndTime}`,
                    ]);
                });

                test("with 'From' date, 'To' date, 'Start time', 'End time'", async ({
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

                    // Check that table data contains the dates/times that were selected
                    await expectTableColumnDateTimeInRange(
                        page,
                        1,
                        fromDate,
                        toDate,
                        defaultStartTime,
                        defaultEndTime,
                    );

                    // Check filter status lists receiver value
                    await filterStatus(page, [
                        `${format(fromDate, "MM/dd/yyyy")}–${format(toDate, "MM/dd/yyyy")}`,
                        `${defaultStartTime}–${defaultEndTime}`,
                    ]);
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

            test.describe("on reset", () => {
                test("form elements clear", async ({ page }) => {
                    await resetButton(page).click();
                    await expect(receiverDropdown(page)).toHaveValue("");
                    await expect(startDate(page)).toHaveValue("");
                    await expect(endDate(page)).toHaveValue("");
                    await expect(startTime(page)).toHaveValue("");
                    await expect(endTime(page)).toHaveValue("");
                });
            });
        });

        test.describe("table", () => {
            test("has correct headers", async ({ page }) => {
                await tableHeaders(page);
            });

            test("has pagination", async ({ page }) => {
                await expect(
                    page.getByTestId("Deliveries pagination"),
                ).toBeAttached();
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
