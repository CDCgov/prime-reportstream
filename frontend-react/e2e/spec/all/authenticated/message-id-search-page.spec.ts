import { expect, test } from "@playwright/test";
import { noData, tableRows } from "../../../helpers/utils";
import { MOCK_GET_MESSAGE, MOCK_GET_MESSAGES } from "../../../mocks/messages";
import * as messageIdSearch from "../../../pages/message-id-search";
import { MESSAGE_ID, URL_MESSAGE_ID_SEARCH } from "../../../pages/message-id-search";
import { openReportIdDetailPage } from "../../../pages/submission-history";
import * as submissionHistory from "../../../pages/submission-history";

test.describe("Message ID Search Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await messageIdSearch.goto(page);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("on search with results", () => {
            test.beforeEach(async ({ page }) => {
                await page.route(messageIdSearch.API_MESSAGES, (route) =>
                    route.fulfill({
                        status: 200,
                        json: MOCK_GET_MESSAGES,
                    }),
                );
                await page.route(messageIdSearch.API_MESSAGE, (route) =>
                    route.fulfill({
                        status: 200,
                        json: MOCK_GET_MESSAGE,
                    }),
                );
                await submissionHistory.mockGetReportHistoryResponse(page);
                await messageIdSearch.goto(page);

                await page.locator("#search-field").fill(MESSAGE_ID);
                await page
                    .getByRole("button", {
                        name: "Search",
                    })
                    .click();
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveURL(URL_MESSAGE_ID_SEARCH);
                await expect(page).toHaveTitle(/Message ID search - Admin/);
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });

            test("displays expected table headers and data", async ({ page }) => {
                // include header row
                const rowCount = MOCK_GET_MESSAGES.length + 1;
                const table = page.getByRole("table");
                await expect(table).toBeVisible();
                const rows = await table.getByRole("row").all();
                expect(rows).toHaveLength(rowCount);

                const colHeaders = ["Message ID", "Sender", "Date/time submitted", "Incoming Report Id"];
                for (const [i, row] of rows.entries()) {
                    const cols = await row.getByRole("cell").allTextContents();
                    expect(cols).toHaveLength(colHeaders.length);

                    const { messageId, sender, submittedDate, reportId } =
                        i === 0
                            ? MOCK_GET_MESSAGES[0]
                            : (MOCK_GET_MESSAGES.find((i) => i.reportId === cols[3]) ?? { reportId: "INVALID" });
                    // if first row, we expect column headers. else, the data row matching the report id
                    const expectedColContents =
                        i === 0
                            ? colHeaders
                            : [
                                  messageId,
                                  sender ?? "",
                                  submittedDate ? new Date(submittedDate).toLocaleString() : "",
                                  reportId ?? "",
                              ];

                    for (const [i, col] of cols.entries()) {
                        expect(col).toBe(expectedColContents[i]);
                    }
                }
            });

            test("table column 'Message ID' will open message id details", async ({ page }) => {
                const messageIdCell = tableRows(page)
                    .nth(0)
                    .locator("td")
                    .nth(0)
                    .getByRole("link", { name: MESSAGE_ID });
                await messageIdCell.click();
                await expect(page).toHaveURL("/message-details/0");
                expect(page.locator("h1").getByText(MESSAGE_ID)).toBeTruthy();
            });

            test("table column 'Incoming Report Id' will open report id details", async ({ page }) => {
                const reportId = "73e3cbc8-9920-4ab7-871f-843a1db4c074";
                const reportIdCell = tableRows(page).nth(0).locator("td").nth(3).getByRole("link", {
                    name: reportId,
                });
                await reportIdCell.click();
                await openReportIdDetailPage(page, reportId);
            });
        });

        test.describe("on search without results", () => {
            test.beforeEach(async ({ page }) => {
                await page.route(messageIdSearch.API_MESSAGES, (route) =>
                    route.fulfill({
                        status: 200,
                        json: [],
                    }),
                );
                await messageIdSearch.goto(page);

                await page.locator("#search-field").fill(MESSAGE_ID);
                await page
                    .getByRole("button", {
                        name: "Search",
                    })
                    .click();
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveURL(URL_MESSAGE_ID_SEARCH);
                await expect(page).toHaveTitle(/Message ID search - Admin/);
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });

            test("shows no data", async ({ page }) => {
                await expect(noData(page)).toBeAttached();
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await messageIdSearch.goto(page);
        });

        test("returns Page Not Found", async ({ page }) => {
            await expect(page).toHaveTitle(/Page Not Found/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await messageIdSearch.goto(page);
        });

        test("returns Page Not Found", async ({ page }) => {
            await expect(page).toHaveTitle(/Page Not Found/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });
});
