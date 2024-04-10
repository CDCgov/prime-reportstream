import { expect, test } from "@playwright/test";
import { selectTestOrg, tableData } from "../helpers/utils";
import { tableHeaders, title } from "../pages/daily-data";
import * as reportDetails from "../pages/report-details";

const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";
const fileName = `hhsprotect-covid-19-73e3cbc8-9920-4ab7-871f-843a1db4c074.csv`;
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
                await reportDetails.mockGetReportDeliveryResponse(page, id);
                await reportDetails.mockGetReportFacilitiesResponse(page, id);
                await reportDetails.goto(page, id);
            });

            test("has correct title", async ({ page }) => {
                await title(page);
            });

            test.describe("table", () => {
                test("has correct headers", async ({ page }) => {
                    await tableHeaders(page);
                });

                test("'Facility' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 0, "Any lab USA 1");
                });

                test("'Location' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 1, "Juneau, AK");
                });

                test("'CLIA' column has expected data", async ({ page }) => {
                    await tableData(page, 0, 2, "34D8574402");
                });

                test("'Total tests' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 3, "10");
                });

                test("'Total positive' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 4, "1");
                });
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test.beforeEach(async ({ page }) => {
                await selectTestOrg(page);
                await reportDetails.mockGetReportDeliveryResponse(page, id);
                await reportDetails.mockGetReportFacilitiesResponse(page, id);
                await reportDetails.goto(page, id);
            });

            test("has correct title", async ({ page }) => {
                await title(page);
            });

            test.describe("table", () => {
                test("has correct headers", async ({ page }) => {
                    await tableHeaders(page);
                });

                test("'Facility' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 0, "Any lab USA 1");
                });

                test("'Location' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 1, "Juneau, AK");
                });

                test("'CLIA' column has expected data", async ({ page }) => {
                    await tableData(page, 0, 2, "34D8574402");
                });

                test("'Total tests' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 3, "10");
                });

                test("'Total positive' column has expected data", async ({
                    page,
                }) => {
                    await tableData(page, 0, 4, "1");
                });
            });

            test("should download file", async ({ page }) => {
                await reportDetails.downloadFile(page, id, fileName);
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });
    });

    test.describe("admin user - server error", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetReportDeliveryResponse(page, id, 500);
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
            await reportDetails.mockGetReportDeliveryResponse(page, id);
            await reportDetails.mockGetReportFacilitiesResponse(page, id);
            await reportDetails.goto(page, id);
        });

        test("has correct title", async ({ page }) => {
            await title(page);
        });

        test.describe("table", () => {
            test("has correct headers", async ({ page }) => {
                await tableHeaders(page);
            });

            test("'Facility' column has expected data", async ({ page }) => {
                await tableData(page, 0, 0, "Any lab USA 1");
            });

            test("'Location' column has expected data", async ({ page }) => {
                await tableData(page, 0, 1, "Juneau, AK");
            });

            test("'CLIA' column has expected data", async ({ page }) => {
                await tableData(page, 0, 2, "34D8574402");
            });

            test("'Total tests' column has expected data", async ({ page }) => {
                await tableData(page, 0, 3, "10");
            });

            test("'Total positive' column has expected data", async ({
                page,
            }) => {
                await tableData(page, 0, 4, "1");
            });
        });

        test("should download file", async ({ page }) => {
            await reportDetails.downloadFile(page, id, fileName);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user - server error", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await reportDetails.mockGetReportDeliveryResponse(page, id, 500);
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
