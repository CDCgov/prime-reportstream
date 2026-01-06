import fs from "node:fs";
import { parseFileLocation } from "../../../../../src/utils/misc";
import { tableRows } from "../../../../helpers/utils";
import { MOCK_GET_MESSAGE } from "../../../../mocks/messages";
import { MessageDetailsPage } from "../../../../pages/authenticated/admin/message-details";
import { MessageIDSearchPage } from "../../../../pages/authenticated/admin/message-id-search";
import { mockGetHistoryReportResponse } from "../../../../pages/authenticated/report-details";

import { test as baseTest, expect } from "../../../../test";

export interface MessageDetailsPageFixtures {
    messageDetailsPage: MessageDetailsPage;
}

const test = baseTest.extend<MessageDetailsPageFixtures>({
    messageDetailsPage: async (
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
        const page = new MessageDetailsPage({
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

test.describe("Message Details Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ messageDetailsPage }) => {
            await expect(messageDetailsPage.page).toHaveURL("/login");
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("Header", () => {
            test("has correct title + heading", async ({ messageDetailsPage }) => {
                await messageDetailsPage.testHeader();
            });
        });

        test("has message id section", async ({ messageDetailsPage }) => {
            await expect(messageDetailsPage.page.getByText("Message ID", { exact: true })).toBeVisible();
            await expect(messageDetailsPage.page.getByText(MessageIDSearchPage.MESSAGE_ID)).toBeVisible();
        });

        test("has sender section", async ({ messageDetailsPage }) => {
            const { sender, reportId, submittedDate } = MOCK_GET_MESSAGE;

            await expect(messageDetailsPage.page.getByText("Sender:")).toBeVisible();
            await expect(messageDetailsPage.page.getByText(sender)).toBeVisible();
            await expect(messageDetailsPage.page.getByText("Incoming Report ID")).toBeVisible();
            await expect(messageDetailsPage.page.getByText(reportId, { exact: true })).toBeVisible();
            await expect(messageDetailsPage.page.getByText("Date/Time Submitted")).toBeVisible();
            await expect(messageDetailsPage.page.getByText(new Date(submittedDate).toLocaleString())).toBeVisible();
            await expect(messageDetailsPage.page.getByText("File Location")).toBeVisible();
            await expect(messageDetailsPage.page.getByText("RECEIVE", { exact: true })).toBeVisible();
            await expect(messageDetailsPage.page.getByText("ignore.ignore-simple-report")).toBeVisible();
            await expect(messageDetailsPage.page.getByText("Incoming File Name")).toBeVisible();
            await expect(
                messageDetailsPage.page.getByText(
                    "pdi-covid-19-d9a57df0-2702-4e28-9d80-ff8c9ec51816-20240514142655.csv",
                ),
            ).toBeVisible();
        });

        test.describe("authenticated admin", () => {
            test("displays expected table headers and data", async ({ messageDetailsPage }) => {
                // include header row
                const rowCount = MOCK_GET_MESSAGE.receiverData.length + 1;
                const table = messageDetailsPage.page.getByRole("table");
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

                    const { receivingOrg, receivingOrgSvc, createdAt, reportId, fileUrl, transportResult } =
                        i === 0
                            ? MOCK_GET_MESSAGE.receiverData[0]
                            : (MOCK_GET_MESSAGE.receiverData.find((i) => i.reportId === cols[3]) ?? {
                                  reportId: "INVALID",
                              });

                    // if first row, we expect column headers. else, the data row matching the report id
                    const expectedColContents =
                        i === 0
                            ? colHeaders
                            : [
                                  receivingOrg ?? "",
                                  receivingOrgSvc ?? "",
                                  createdAt ? new Date(createdAt).toLocaleString() : "",
                                  reportId,
                                  parseFileLocation(fileUrl ?? "N/A").folderLocation.toLocaleUpperCase(),
                                  parseFileLocation(fileUrl ?? "N/A").sendingOrg,
                                  parseFileLocation(fileUrl ?? "N/A").fileName,
                                  transportResult ?? "",
                              ];

                    for (const [i, col] of cols.entries()) {
                        expect(col).toBe(expectedColContents[i]);
                    }
                }
            });

            test("table column 'FileName' will download file", async ({ messageDetailsPage }) => {
                const downloadProm = messageDetailsPage.page.waitForEvent("download");
                await mockGetHistoryReportResponse(messageDetailsPage.page, "*");

                await tableRows(messageDetailsPage.page).nth(0).locator("td").nth(6).getByRole("button").click();

                const download = await downloadProm;

                // assert filename
                expect(download.suggestedFilename()).toBe(
                    "hhsprotect-covid-19-73e3cbc8-9920-4ab7-871f-843a1db4c074.csv",
                );
                // get and assert stats
                expect((await fs.promises.stat(await download.path())).size).toBeGreaterThan(200);
            });
        });

        test.describe("Footer", () => {
            test("has footer and explicit scroll to footer and scroll to top", async ({ messageDetailsPage }) => {
                await messageDetailsPage.testFooter();
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test("has alert", async ({ messageDetailsPage }) => {
            messageDetailsPage.mockError = true;
            await messageDetailsPage.reload();

            await expect(messageDetailsPage.page.getByTestId("alert")).toBeAttached();
            await expect(
                messageDetailsPage.page.getByText(/Our apologies, there was an error loading this content./),
            ).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test("has alert", async ({ messageDetailsPage }) => {
            messageDetailsPage.mockError = true;
            await messageDetailsPage.reload();

            await expect(messageDetailsPage.page.getByTestId("alert")).toBeAttached();
            await expect(
                messageDetailsPage.page.getByText(/Our apologies, there was an error loading this content./),
            ).toBeAttached();
        });
    });
});
