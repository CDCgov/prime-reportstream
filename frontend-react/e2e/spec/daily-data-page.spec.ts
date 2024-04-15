import { expect, test } from "@playwright/test";

import { selectTestOrg, waitForAPIResponse } from "../helpers/utils";
import * as dailyData from "../pages/daily-data";
import { mockGetDeliveriesForOrgResponse } from "../pages/report-details";

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
            await mockGetDeliveriesForOrgResponse(page, "ak-phd.elr", "AK");
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
                //         await expect(receivers).toHaveText(" ");
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
                test("table loads with selected receiver", async ({ page }) => {
                    await page
                        .locator("#receiver-dropdown")
                        .selectOption("elr");
                    await page
                        .getByRole("button", {
                            name: "Apply",
                        })
                        .click();
                    // Check that table data contains the receiver selected ???

                    //Check filter status lists receiver value
                    await expect(
                        page.getByTestId("filter-status"),
                    ).toContainText("elr");
                });

                test("with 'From' date", async ({ page }) => {
                    await expect(page.locator("#start-date")).toHaveValue("");

                    // Apply button is disabled
                    await page.locator("#start-date").fill("4/12/2024");
                    await page.keyboard.press("Tab");
                    await expect(page.locator("#start-date")).toHaveValue(
                        "4/12/2024",
                    );
                    const applyButton = page.getByRole("button", {
                        name: "Apply",
                    });
                    await expect(applyButton).toHaveAttribute("disabled");

                    // click on reset clears date
                    await page
                        .getByTestId("filter-form")
                        .getByRole("button", { name: "Reset" })
                        .click();
                    await expect(page.locator("#start-date")).toHaveValue("");
                });

                test("with 'From' date and 'Start' time", () => {
                    // Apply button is disabled
                    // click on X clears time
                    // click on reset clears date and time
                });

                test("with 'To' date", () => {});

                test("with 'To' date and 'End' time", () => {});

                test("with 'From' date and 'To' date", () => {
                    // Apply button is enabled
                    // click on reset clears dates
                    // Check table
                });

                test("with 'Start' time and 'End' time", () => {
                    // Apply button is disabled
                });

                test("with 'From' date and 'To' date and 'Start' time and 'End' time", () => {
                    // Apply button is enabled
                    // click on reset clears dates
                    // Check table
                });
            });

            test.describe("with date", () => {});

            test.describe("with date and time", () => {});
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
