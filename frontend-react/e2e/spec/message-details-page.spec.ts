import { expect, test } from "@playwright/test";
import fs from "node:fs";
import { parseFileLocation } from "../../src/utils/misc";
import { tableRows } from "../helpers/utils";
import { MOCK_GET_MESSAGE } from "../mocks/messages";
import * as messageDetails from "../pages/message-details";
import { URL_MESSAGE_DETAILS } from "../pages/message-details";
import * as messageIdSearch from "../pages/message-id-search";
import { MESSAGE_ID } from "../pages/message-id-search";
import { mockGetHistoryReportResponse } from "../pages/report-details";
test.describe("Message Details Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await messageDetails.goto(page);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.beforeEach(async ({ page }) => {
            await page.route(messageIdSearch.API_MESSAGE, (route) =>
                route.fulfill({
                    status: 200,
                    json: MOCK_GET_MESSAGE,
                }),
            );
            await messageDetails.goto(page);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveURL(URL_MESSAGE_DETAILS);
            await expect(page).toHaveTitle(
                /ReportStream - CDC's free, interoperable data transfer platform/,
            );
        });

        test("has message id section", async ({ page }) => {
            await expect(
                page.getByText("Message ID", { exact: true }),
            ).toBeVisible();
            await expect(page.getByText(MESSAGE_ID)).toBeVisible();
        });

        test("has sender section", async ({ page }) => {
            const { sender, reportId, submittedDate } = MOCK_GET_MESSAGE;

            await expect(page.getByText("Sender:")).toBeVisible();
            await expect(page.getByText(sender)).toBeVisible();
            await expect(page.getByText("Incoming Report ID")).toBeVisible();
            await expect(
                page.getByText(reportId, { exact: true }),
            ).toBeVisible();
            await expect(page.getByText("Date/Time Submitted")).toBeVisible();
            await expect(
                page.getByText(new Date(submittedDate).toLocaleString()),
            ).toBeVisible();
            await expect(page.getByText("File Location")).toBeVisible();
            await expect(
                page.getByText("RECEIVE", { exact: true }),
            ).toBeVisible();
            await expect(
                page.getByText("ignore.ignore-simple-report"),
            ).toBeVisible();
            await expect(page.getByText("Incoming File Name")).toBeVisible();
            await expect(
                page.getByText(
                    "pdi-covid-19-d9a57df0-2702-4e28-9d80-ff8c9ec51816-20240514142655.csv",
                ),
            ).toBeVisible();
        });

        test.describe("authenticated admin", () => {
            test("has receiver title", async ({ page }) => {
                await expect(page.getByText("Receivers:")).toBeVisible();
            });

            test("displays expected table headers and data", async ({
                page,
            }) => {
                // include header row
                const rowCount = MOCK_GET_MESSAGE.receiverData.length + 1;
                const table = page.getByRole("table");
                await expect(table).toBeVisible();
                const rows = await table.getByRole("row").all();
                expect(rows).toHaveLength(rowCount);

                const colHeaders = [
                    "Name",
                    "Service",
                    "Date",
                    "Report Id",
                    "Main",
                    "Sub",
                    "File Name",
                    "Transport Results",
                ];
                for (const [i, row] of rows.entries()) {
                    const cols = await row.getByRole("cell").allTextContents();
                    expect(cols).toHaveLength(colHeaders.length);

                    const {
                        receivingOrg,
                        receivingOrgSvc,
                        createdAt,
                        reportId,
                        fileUrl,
                        transportResult,
                    } =
                        i === 0
                            ? MOCK_GET_MESSAGE.receiverData[0]
                            : MOCK_GET_MESSAGE.receiverData.find(
                                  (i) => i.reportId === cols[3],
                              ) ?? { reportId: "INVALID" };

                    // if first row, we expect column headers. else, the data row matching the report id
                    const expectedColContents =
                        i === 0
                            ? colHeaders
                            : [
                                  receivingOrg ?? "",
                                  receivingOrgSvc ?? "",
                                  createdAt
                                      ? new Date(createdAt).toLocaleString()
                                      : "",
                                  reportId,
                                  parseFileLocation(
                                      fileUrl ?? "N/A",
                                  ).folderLocation.toLocaleUpperCase(),
                                  parseFileLocation(fileUrl ?? "N/A")
                                      .sendingOrg,
                                  parseFileLocation(fileUrl ?? "N/A").fileName,
                                  transportResult ?? "",
                              ];

                    for (const [i, col] of cols.entries()) {
                        expect(col).toBe(expectedColContents[i]);
                    }
                }
            });

            test("table column 'FileName' will download file", async ({
                page,
            }) => {
                const downloadProm = page.waitForEvent("download");
                await mockGetHistoryReportResponse(page, "*");

                await tableRows(page)
                    .nth(0)
                    .locator("td")
                    .nth(6)
                    .getByRole("button")
                    .click();

                const download = await downloadProm;

                // assert filename
                expect(download.suggestedFilename()).toBe(
                    "hhsprotect-covid-19-73e3cbc8-9920-4ab7-871f-843a1db4c074.csv",
                );
                // get and assert stats
                expect(
                    (await fs.promises.stat(await download.path())).size,
                ).toBeGreaterThan(200);
            });
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await messageDetails.goto(page);
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
            await messageDetails.goto(page);
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
