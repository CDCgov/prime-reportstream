import { pageNotFound } from "../../../../../src/content/error/ErrorMessages";
import { noData, tableRows } from "../../../../helpers/utils";
import { MOCK_GET_MESSAGES } from "../../../../mocks/messages";
import { MessageIDSearchPage } from "../../../../pages/authenticated/admin/message-id-search";
import { openReportIdDetailPage } from "../../../../pages/authenticated/submission-history";

import { test as baseTest, expect } from "../../../../test";

export interface MessageIDSearchPageFixtures {
    messageIDSearchPage: MessageIDSearchPage;
}

const test = baseTest.extend<MessageIDSearchPageFixtures>({
    messageIDSearchPage: async (
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
        const page = new MessageIDSearchPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
        });
        await page.goto();
        await use(page);
    },
});

test.describe("Message ID Search Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ messageIDSearchPage }) => {
            await expect(messageIDSearchPage.page).toHaveURL("/login");
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });
        test.beforeEach(async ({ messageIDSearchPage }) => {
            await messageIDSearchPage.page.locator("#search-field").fill(MessageIDSearchPage.MESSAGE_ID);
            await messageIDSearchPage.page
                .getByRole("button", {
                    name: "Search",
                })
                .click();
        });

        test.describe("on search with results", () => {
            test.describe("Header", () => {
                test("has correct title + heading", async ({ messageIDSearchPage }) => {
                    await messageIDSearchPage.testHeader();
                });
            });

            test("displays expected table headers and data", async ({ messageIDSearchPage }) => {
                // include header row
                const rowCount = MOCK_GET_MESSAGES.length + 1;
                const table = messageIDSearchPage.page.getByRole("table");
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

            test("table column 'Message ID' will open message id details", async ({ messageIDSearchPage }) => {
                const messageIdCell = tableRows(messageIDSearchPage.page)
                    .nth(0)
                    .locator("td")
                    .nth(0)
                    .getByRole("link", { name: MessageIDSearchPage.MESSAGE_ID });
                await messageIdCell.click();
                await expect(messageIDSearchPage.page).toHaveURL("/message-details/0");
                expect(messageIDSearchPage.page.locator("h1").getByText(MessageIDSearchPage.MESSAGE_ID)).toBeTruthy();
            });

            test("table column 'Incoming Report Id' will open report id details", async ({ messageIDSearchPage }) => {
                const reportId = "73e3cbc8-9920-4ab7-871f-843a1db4c074";
                const reportIdCell = tableRows(messageIDSearchPage.page).nth(0).locator("td").nth(3).getByRole("link", {
                    name: reportId,
                });
                await reportIdCell.click();
                await openReportIdDetailPage(messageIDSearchPage.page, reportId);
            });
        });

        test.describe("on search without results", () => {
            test.beforeEach(async ({ messageIDSearchPage }) => {
                await messageIDSearchPage.page.route(MessageIDSearchPage.API_MESSAGES, (route) =>
                    route.fulfill({
                        status: 200,
                        json: [],
                    }),
                );
                await messageIDSearchPage.page.goto(MessageIDSearchPage.URL_MESSAGE_ID_SEARCH);

                await messageIDSearchPage.page.locator("#search-field").fill(MessageIDSearchPage.MESSAGE_ID);
                await messageIDSearchPage.page
                    .getByRole("button", {
                        name: "Search",
                    })
                    .click();
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveURL(MessageIDSearchPage.URL_MESSAGE_ID_SEARCH);
                await expect(page).toHaveTitle(/Message ID search - Admin/);
            });

            test("shows no data", async ({ page }) => {
                await expect(noData(page)).toBeAttached();
            });
        });

        test.describe("Footer", () => {
            test("has footer and explicit scroll to footer and scroll to top", async ({ messageIDSearchPage }) => {
                await messageIDSearchPage.testFooter();
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test("has alert", async ({ messageIDSearchPage }) => {
            messageIDSearchPage.mockError = true;
            await messageIDSearchPage.reload();

            await expect(messageIDSearchPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test("has alert", async ({ messageIDSearchPage }) => {
            messageIDSearchPage.mockError = true;
            await messageIDSearchPage.reload();

            await expect(messageIDSearchPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });
});
