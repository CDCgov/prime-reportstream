import { expect, test } from "@playwright/test";

import { format } from "date-fns";
import {
    expectTableColumnDateInRange,
    expectTableColumnValues,
    getTableRowCount,
    selectTestOrg,
    waitForAPIResponse,
} from "../helpers/utils";
import * as dailyData from "../pages/daily-data";
import { mockGetDeliveriesForOrgResponse } from "../pages/report-details";

const selectedReceiver = "elr";
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

            test("has receiver services dropdown", async ({ page }) => {
                await expect(page.locator("#receiver-dropdown")).toBeAttached();
            });

            test("has filter", async ({ page }) => {
                await expect(page.getByTestId("filter-form")).toBeAttached();
            });

            test.describe("table", () => {
                test("has correct headers", async ({ page }) => {
                    await expect(
                        page.locator(".usa-table th").nth(0),
                    ).toHaveText(/Report ID/);
                    await expect(
                        page.locator(".usa-table th").nth(1),
                    ).toHaveText(/Time received/);
                    await expect(
                        page.locator(".usa-table th").nth(2),
                    ).toHaveText(/File available until/);
                    await expect(
                        page.locator(".usa-table th").nth(3),
                    ).toHaveText(/Items/);
                    await expect(
                        page.locator(".usa-table th").nth(4),
                    ).toHaveText(/Filename/);
                    await expect(
                        page.locator(".usa-table th").nth(5),
                    ).toHaveText(/Receiver/);
                });

                test("has pagination", async ({ page }) => {
                    await expect(
                        page.getByTestId("Deliveries pagination"),
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
            // Will need to use mock data until staging can support live data for receiver
            await mockGetDeliveriesForOrgResponse(
                page,
                `ak-phd.${selectedReceiver}`,
                "AK",
            );
            await dailyData.goto(page);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Daily Data - ReportStream/);
        });

        test.describe("filter", () => {
            test("has filter", async ({ page }) => {
                await expect(page.getByTestId("filter-form")).toBeAttached();
            });

            test.describe("onLoad", () => {
                // test.describe("receiver dropdown", () => {
                //     test("onLoad does not have a receiver selected", async ({
                //         page,
                //     }) => {
                //         const receivers = page.locator("#receiver-dropdown");
                //         await expect(receivers).toBeAttached();
                //         await expect(receivers).toHaveText("");
                //     });
                // });

                test("'From' date does not have a value", async ({ page }) => {
                    const startDate = page.locator("#start-date");
                    await expect(startDate).toBeAttached();
                    await expect(startDate).toHaveValue("");
                });

                test("'To' date does not have a value", async ({ page }) => {
                    const endDate = page.locator("#end-date");
                    await expect(endDate).toBeAttached();
                    await expect(endDate).toHaveValue("");
                });

                test("'Start time' does not have a value", async ({ page }) => {
                    const startTime = page.locator("#start-time");
                    await expect(startTime).toBeAttached();
                    await expect(startTime).toHaveText("");
                });

                test("'End time'' does not have a value", async ({ page }) => {
                    const endTime = page.locator("#end-time");
                    await expect(endTime).toBeAttached();
                    await expect(endTime).toHaveText("");
                });
            });

            test.describe("with receiver", () => {
                test.beforeEach(async ({ page }) => {
                    await page
                        .locator("#receiver-dropdown")
                        .selectOption(selectedReceiver);
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
                        `ak-phd.${selectedReceiver}`,
                    );

                    // Check filter status lists receiver value
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(selectedReceiver);

                    // Receiver dropdown persists
                    await expect(
                        page.locator("#receiver-dropdown"),
                    ).toHaveValue(selectedReceiver);
                });

                test("with 'From' date", async ({ page }) => {
                    await dailyData.setDate(page, "#start-date", 7);

                    // Apply button is disabled
                    const applyButton = page.getByRole("button", {
                        name: "Apply",
                    });
                    await expect(applyButton).toHaveAttribute("disabled");

                    // click on reset clears date
                    // await page
                    //     .getByTestId("filter-form")
                    //     .getByRole("button", { name: "Reset" })
                    //     .click();
                    // await expect(page.locator("#start-date")).toHaveValue("");
                });

                test("with 'To' date", async ({ page }) => {
                    await expect(page.locator("#end-date")).toHaveValue("");

                    await dailyData.setDate(page, "#end-date", 7);

                    // Apply button is disabled
                    const applyButton = page.getByRole("button", {
                        name: "Apply",
                    });
                    await expect(applyButton).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'Start' time", async ({ page }) => {
                    await dailyData.setDate(page, "#start-date", 7);
                    await dailyData.setTime(
                        page,
                        "[name=start-time]",
                        "6:00am",
                    );

                    // Apply button is disabled
                    const applyButton = page.getByRole("button", {
                        name: "Apply",
                    });
                    await expect(applyButton).toHaveAttribute("disabled");
                });

                test("with 'From' date and 'End' time", async ({ page }) => {
                    await dailyData.setDate(page, "#start-date", 7);
                    await dailyData.setTime(page, "[name=end-time]", "8:00pm");

                    // Apply button is disabled
                    const applyButton = page.getByRole("button", {
                        name: "Apply",
                    });
                    await expect(applyButton).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'End' time", async ({ page }) => {
                    await dailyData.setDate(page, "#end-date", 7);
                    await dailyData.setTime(page, "[name=end-time]", "8:00pm");

                    // Apply button is disabled
                    const applyButton = page.getByRole("button", {
                        name: "Apply",
                    });
                    await expect(applyButton).toHaveAttribute("disabled");
                });

                test("with 'To' date and 'Start' time", async ({ page }) => {
                    await dailyData.setDate(page, "#end-date", 7);
                    await dailyData.setTime(
                        page,
                        "[name=start-time]",
                        "6:00am",
                    );

                    // Apply button is disabled
                    const applyButton = page.getByRole("button", {
                        name: "Apply",
                    });
                    await expect(applyButton).toHaveAttribute("disabled");
                });

                // test("with 'Start' time and 'End' time", async ({ page }) => {
                //     await dailyData.setStartTime(page, "6:00am");
                //     await dailyData.setTime(page, "[name=end-time]", "8:00pm");
                //
                //     // Apply button is disabled
                //     const applyButton = page.getByRole("button", {
                //         name: "Apply",
                //     });
                //     await expect(applyButton).toHaveAttribute("disabled");
                // });

                test("with 'From' date and 'To' date", async ({ page }) => {
                    const fromDate = await dailyData.setDate(
                        page,
                        "#start-date",
                        7,
                    );
                    const toDate = await dailyData.setDate(
                        page,
                        "#end-date",
                        0,
                    );

                    // Apply button is enabled
                    await page
                        .getByRole("button", {
                            name: "Apply",
                        })
                        .click();

                    // Check that table data contains the 'From' date that was selected
                    // await expectTableColumnDateInRange(
                    //     page,
                    //     1,
                    //     new Date(fromDate),
                    //     new Date(toDate),
                    // );

                    const rowCount = await getTableRowCount(page);
                    // Check filter status lists receiver value
                    await expect(page.getByTestId("filter-status")).toHaveText(
                        `Showing (${rowCount}) result for: ${selectedReceiver}, ${format(fromDate, "MM/dd/yyyy")}-${format(toDate, "MM/dd/yyyy")}`,
                    );
                });

                test("with 'From' date, 'To' date, 'Start' time, 'End' time", async ({
                    page,
                }) => {
                    const fromDate = await dailyData.setDate(
                        page,
                        "#start-date",
                        7,
                    );
                    const toDate = await dailyData.setDate(
                        page,
                        "#end-date",
                        0,
                    );
                    await dailyData.setTime(
                        page,
                        "[name=start-time]",
                        "6:00am",
                    );
                    await dailyData.setTime(page, "[name=end-time]", "8:00pm");

                    // Apply button is enabled
                    await page
                        .getByRole("button", {
                            name: "Apply",
                        })
                        .click();

                    // Check that table data is between the 'From' date and 'To date that was selected
                    // TODO: need to add time component here
                    await expectTableColumnDateInRange(
                        page,
                        1,
                        new Date(`${fromDate}`),
                        new Date(`${toDate}`),
                    );

                    // Check filter status lists receiver value
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText(
                        `${selectedReceiver}, ${fromDate}-${toDate}, "6:00am-8:00pm"`,
                    );
                });
            });

            test.describe("no receiver", () => {
                test.describe("with date", () => {});

                test.describe("with date and time", () => {});
            });
        });

        test.describe("table", () => {
            test("has correct headers", async ({ page }) => {
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

            test("has pagination", async ({ page }) => {
                await expect(
                    page.getByTestId("Deliveries pagination"),
                ).toBeAttached();
            });
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
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
