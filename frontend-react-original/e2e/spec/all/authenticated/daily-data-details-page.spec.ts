import { expect } from "@playwright/test";
import { tableDataCellValue } from "../../../helpers/utils";
import { detailsTableHeaders } from "../../../pages/authenticated/daily-data";
import { DailyDataDetailsPage } from "../../../pages/authenticated/daily-data-details";
import * as reportDetails from "../../../pages/authenticated/report-details";
import { test as baseTest } from "../../../test";

export interface DailyDataDetailsPageFixtures {
    dailyDataDetailsPage: DailyDataDetailsPage;
}

const test = baseTest.extend<DailyDataDetailsPageFixtures>({
    dailyDataDetailsPage: async (
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
        const page = new DailyDataDetailsPage({
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
const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";
const fileName = `hhsprotect-covid-19-73e3cbc8-9920-4ab7-871f-843a1db4c074.csv`;

test.describe("Daily Data Details page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ dailyDataDetailsPage }) => {
            await expect(dailyDataDetailsPage.page).toHaveURL("/login");
        });
    });

    test.describe("admin user - happy path", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ dailyDataDetailsPage }) => {
                await dailyDataDetailsPage.page.getByRole("table").waitFor({ state: "visible" });
            });

            test.describe("table", () => {
                test("has correct headers", async ({ dailyDataDetailsPage }) => {
                    await detailsTableHeaders(dailyDataDetailsPage.page);
                });

                test("'Facility' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 0)).toEqual("Any lab USA 1");
                });

                test("'Location' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 1)).toEqual("Juneau, AK");
                });

                test("'CLIA' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 2)).toEqual("34D8574402");
                });

                test("'Total tests' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 3)).toEqual("10");
                });

                test("'Total positive' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 4)).toEqual("1");
                });
            });

            test("has footer", async ({ dailyDataDetailsPage }) => {
                await expect(dailyDataDetailsPage.page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test.beforeEach(async ({ dailyDataDetailsPage }) => {
                await dailyDataDetailsPage.page.getByRole("table").waitFor({ state: "visible" });
            });

            test("has correct title", async ({ dailyDataDetailsPage }) => {
                await expect(dailyDataDetailsPage.page).toHaveTitle(dailyDataDetailsPage.title);
            });

            test.describe("table", () => {
                test("has correct headers", async ({ dailyDataDetailsPage }) => {
                    await detailsTableHeaders(dailyDataDetailsPage.page);
                });

                test("'Facility' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 0)).toEqual("Any lab USA 1");
                });

                test("'Location' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 1)).toEqual("Juneau, AK");
                });

                test("'CLIA' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 2)).toEqual("34D8574402");
                });

                test("'Total tests' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 3)).toEqual("10");
                });

                test("'Total positive' column has expected data", async ({ dailyDataDetailsPage }) => {
                    expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 4)).toEqual("1");
                });
            });

            test("should download file", async ({ dailyDataDetailsPage }) => {
                await reportDetails.downloadFile(dailyDataDetailsPage.page, id, fileName);
            });

            test("has footer", async ({ dailyDataDetailsPage }) => {
                await expect(dailyDataDetailsPage.page.locator("footer")).toBeAttached();
            });
        });
    });

    test.describe("admin user - server error", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.beforeEach(async ({ dailyDataDetailsPage }) => {
            await reportDetails.mockGetReportDeliveryResponse(dailyDataDetailsPage.page, id, 500);
            await reportDetails.goto(dailyDataDetailsPage.page, id);
        });

        test("has alert", async ({ dailyDataDetailsPage }) => {
            // TODO: Fix - mockError is undefined
            //dailyDataDetailsPage.mockError = true;
            // await dailyDataDetailsPage.reload();
            await expect(dailyDataDetailsPage.page.getByTestId("alert")).toBeAttached();
            await expect(
                dailyDataDetailsPage.page.getByText(/Our apologies, there was an error loading this content./),
            ).toBeVisible();
        });

        test("has footer", async ({ dailyDataDetailsPage }) => {
            await expect(dailyDataDetailsPage.page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user - happy path", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ dailyDataDetailsPage }) => {
            await dailyDataDetailsPage.page.getByRole("table").waitFor({ state: "visible" });
        });

        test("has correct title", async ({ dailyDataDetailsPage }) => {
            await expect(dailyDataDetailsPage.page).toHaveTitle(dailyDataDetailsPage.title);
        });

        test.describe("table", () => {
            test("has correct headers", async ({ dailyDataDetailsPage }) => {
                await detailsTableHeaders(dailyDataDetailsPage.page);
            });

            test("'Facility' column has expected data", async ({ dailyDataDetailsPage }) => {
                expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 0)).toEqual("Any lab USA 1");
            });

            test("'Location' column has expected data", async ({ dailyDataDetailsPage }) => {
                expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 1)).toEqual("Juneau, AK");
            });

            test("'CLIA' column has expected data", async ({ dailyDataDetailsPage }) => {
                expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 2)).toEqual("34D8574402");
            });

            test("'Total tests' column has expected data", async ({ dailyDataDetailsPage }) => {
                expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 3)).toEqual("10");
            });

            test("'Total positive' column has expected data", async ({ dailyDataDetailsPage }) => {
                expect(await tableDataCellValue(dailyDataDetailsPage.page, 0, 4)).toEqual("1");
            });
        });

        test("should download file", async ({ dailyDataDetailsPage }) => {
            await reportDetails.downloadFile(dailyDataDetailsPage.page, id, fileName);
        });

        test("has footer", async ({ dailyDataDetailsPage }) => {
            await expect(dailyDataDetailsPage.page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user - server error", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ dailyDataDetailsPage }) => {
            await reportDetails.mockGetReportDeliveryResponse(dailyDataDetailsPage.page, id, 500);
            await reportDetails.goto(dailyDataDetailsPage.page, id);
        });

        test("has alert", async ({ dailyDataDetailsPage }) => {
            await expect(dailyDataDetailsPage.page.getByTestId("alert")).toBeAttached();
            await expect(
                dailyDataDetailsPage.page.getByText(/Our apologies, there was an error loading this content./),
            ).toBeAttached();
        });

        test("has footer", async ({ dailyDataDetailsPage }) => {
            await expect(dailyDataDetailsPage.page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test("has alert", async ({ dailyDataDetailsPage }) => {
            dailyDataDetailsPage.mockError = true;
            await dailyDataDetailsPage.reload();

            await expect(dailyDataDetailsPage.page.getByTestId("alert")).toBeAttached();
            await expect(
                dailyDataDetailsPage.page.getByText(/Our apologies, there was an error loading this content./),
            ).toBeAttached();
        });

        test("has footer", async ({ dailyDataDetailsPage }) => {
            await expect(dailyDataDetailsPage.page.locator("footer")).toBeAttached();
        });
    });
});
