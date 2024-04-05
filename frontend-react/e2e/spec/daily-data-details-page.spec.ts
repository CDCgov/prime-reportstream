import { expect, test } from "@playwright/test";
import fs from "fs";
import { selectTestOrg } from "../helpers/utils";
import * as reportDetails from "../pages/report-details";

const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";

test.describe("Daily Data Details page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await reportDetails.goto(page, id);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("admin user - happy path", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await reportDetails.mockGetDeliveryResponse(page, id);
                await reportDetails.mockGetFacilitiesResponse(page, id);
                await reportDetails.goto(page, id);
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveTitle(
                    /ReportStream - CDC's free, interoperable data transfer platform/,
                );
            });

            test.describe("table", () => {
                test("has correct headers", async ({ page }) => {
                    await expect(
                        page.locator(".usa-table th").nth(0),
                    ).toHaveText(/Facility/);
                    await expect(
                        page.locator(".usa-table th").nth(1),
                    ).toHaveText(/Location/);
                    await expect(
                        page.locator(".usa-table th").nth(2),
                    ).toHaveText(/CLIA/);
                    await expect(
                        page.locator(".usa-table th").nth(3),
                    ).toHaveText(/Total tests/);
                    await expect(
                        page.locator(".usa-table th").nth(4),
                    ).toHaveText(/Total positive/);
                });

                test("'Facility' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(0),
                    ).toHaveText("Any lab USA 1");
                });

                test("'Location' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(1),
                    ).toHaveText("Juneau, AK");
                });

                test("'CLIA' column has expected data", async ({ page }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(2),
                    ).toHaveText("34D8574402");
                });

                test("'Total tests' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(3),
                    ).toHaveText("10");
                });

                test("'Total positive' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(4),
                    ).toHaveText("1");
                });
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test.beforeEach(async ({ page }) => {
                await selectTestOrg(page);
                await reportDetails.mockGetDeliveryResponse(page, id);
                await reportDetails.mockGetFacilitiesResponse(page, id);
                await reportDetails.goto(page, id);
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveTitle(
                    /ReportStream - CDC's free, interoperable data transfer platform/,
                );
            });

            test.describe("table", () => {
                test("has correct headers", async ({ page }) => {
                    await expect(
                        page.locator(".usa-table th").nth(0),
                    ).toHaveText(/Facility/);
                    await expect(
                        page.locator(".usa-table th").nth(1),
                    ).toHaveText(/Location/);
                    await expect(
                        page.locator(".usa-table th").nth(2),
                    ).toHaveText(/CLIA/);
                    await expect(
                        page.locator(".usa-table th").nth(3),
                    ).toHaveText(/Total tests/);
                    await expect(
                        page.locator(".usa-table th").nth(4),
                    ).toHaveText(/Total positive/);
                });

                test("'Facility' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(0),
                    ).toHaveText("Any lab USA 1");
                });

                test("'Location' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(1),
                    ).toHaveText("Juneau, AK");
                });

                test("'CLIA' column has expected data", async ({ page }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(2),
                    ).toHaveText("34D8574402");
                });

                test("'Total tests' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(3),
                    ).toHaveText("10");
                });

                test("'Total positive' column has expected data", async ({
                    page,
                }) => {
                    await expect(
                        page
                            .locator(".usa-table tbody")
                            .locator("tr")
                            .nth(0)
                            .locator("td")
                            .nth(4),
                    ).toHaveText("1");
                });
            });

            test("should download file", async ({ page }) => {
                await reportDetails.mockGetHistoryReportResponse(page, id);
                const [download] = await Promise.all([
                    // Start waiting for the download
                    page.waitForEvent("download"),
                    // Perform the action that initiates download
                    await page.getByRole("button", { name: "CSV" }).click(),
                ]);

                // assert filename
                expect(download.suggestedFilename()).toBe(
                    `hhsprotect-covid-19-${id}.csv`,
                );
                // get and assert stats
                expect(
                    (await fs.promises.stat(await download.path())).size,
                ).toBeGreaterThan(200);
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });
    });

    test.describe("admin user - server error", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetDeliveryResponse(page, id, 500);
            await reportDetails.goto(page, id);
        });

        test("has alert", async ({ page }) => {
            await expect(page.getByTestId("alert")).toBeAttached();
            await expect(
                page.getByText(
                    /Our apologies, there was an error loading this content./,
                ),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user - happy path", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetDeliveryResponse(page, id);
            await reportDetails.mockGetFacilitiesResponse(page, id);
            await reportDetails.goto(page, id);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(
                /ReportStream - CDC's free, interoperable data transfer platform/,
            );
        });

        test.describe("table", () => {
            test("has correct headers", async ({ page }) => {
                await expect(page.locator(".usa-table th").nth(0)).toHaveText(
                    /Facility/,
                );
                await expect(page.locator(".usa-table th").nth(1)).toHaveText(
                    /Location/,
                );
                await expect(page.locator(".usa-table th").nth(2)).toHaveText(
                    /CLIA/,
                );
                await expect(page.locator(".usa-table th").nth(3)).toHaveText(
                    /Total tests/,
                );
                await expect(page.locator(".usa-table th").nth(4)).toHaveText(
                    /Total positive/,
                );
            });

            test("'Facility' column has expected data", async ({ page }) => {
                await expect(
                    page
                        .locator(".usa-table tbody")
                        .locator("tr")
                        .nth(0)
                        .locator("td")
                        .nth(0),
                ).toHaveText("Any lab USA 1");
            });

            test("'Location' column has expected data", async ({ page }) => {
                await expect(
                    page
                        .locator(".usa-table tbody")
                        .locator("tr")
                        .nth(0)
                        .locator("td")
                        .nth(1),
                ).toHaveText("Juneau, AK");
            });

            test("'CLIA' column has expected data", async ({ page }) => {
                await expect(
                    page
                        .locator(".usa-table tbody")
                        .locator("tr")
                        .nth(0)
                        .locator("td")
                        .nth(2),
                ).toHaveText("34D8574402");
            });

            test("'Total tests' column has expected data", async ({ page }) => {
                await expect(
                    page
                        .locator(".usa-table tbody")
                        .locator("tr")
                        .nth(0)
                        .locator("td")
                        .nth(3),
                ).toHaveText("10");
            });

            test("'Total positive' column has expected data", async ({
                page,
            }) => {
                await expect(
                    page
                        .locator(".usa-table tbody")
                        .locator("tr")
                        .nth(0)
                        .locator("td")
                        .nth(4),
                ).toHaveText("1");
            });
        });

        test("should download file", async ({ page }) => {
            await reportDetails.mockGetHistoryReportResponse(page, id);
            const [download] = await Promise.all([
                // Start waiting for the download
                page.waitForEvent("download"),
                // Perform the action that initiates download
                await page.getByRole("button", { name: "CSV" }).click(),
            ]);

            // assert filename
            expect(download.suggestedFilename()).toBe(
                `hhsprotect-covid-19-${id}.csv`,
            );
            // get and assert stats
            expect(
                (await fs.promises.stat(await download.path())).size,
            ).toBeGreaterThan(200);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user - server error", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetDeliveryResponse(page, id, 500);
            await reportDetails.goto(page, id);
        });

        test("has alert", async ({ page }) => {
            await expect(page.getByTestId("alert")).toBeAttached();
            await expect(
                page.getByText(
                    /Our apologies, there was an error loading this content./,
                ),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.goto(page, id);
        });

        test("has alert", async ({ page }) => {
            await expect(page.getByTestId("alert")).toBeAttached();
            await expect(
                page.getByText(
                    /Our apologies, there was an error loading this content./,
                ),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });
});
