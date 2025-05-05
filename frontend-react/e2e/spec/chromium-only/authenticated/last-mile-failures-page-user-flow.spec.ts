import { tableRows } from "../../../helpers/utils";
import { LastMileFailuresPage } from "../../../pages/authenticated/admin/last-mile-failures";
import { test as baseTest, expect } from "../../../test";

export interface LastMileFailuresPageFixtures {
    lastMileFailuresPage: LastMileFailuresPage;
}

const test = baseTest.extend<LastMileFailuresPageFixtures>({
    lastMileFailuresPage: async (
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
        const page = new LastMileFailuresPage({
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

test.describe(
    "Last Mile Failure page",
    {
        tag: "@smoke",
    },
    () => {
        test.describe("admin user", () => {
            test.use({ storageState: "e2e/.auth/admin.json" });

            test.describe("'Filter'", () => {
                test("table has expected data when filtering by 'ReportId'", async ({ lastMileFailuresPage }) => {
                    const reportId = await tableRows(lastMileFailuresPage.page).nth(0).locator("td").nth(1).innerText();
                    await lastMileFailuresPage.filterFormInputs.filter.input.fill(reportId);
                    const isReportIdReturned = await lastMileFailuresPage.testReportId(reportId);
                    expect(isReportIdReturned).toBe(true);
                });
            });

            test.describe("'Days to show' filter", () => {
                test.beforeEach(async ({ lastMileFailuresPage }) => {
                    await lastMileFailuresPage.filterFormInputs.daysToShow.input.fill("200");
                    await lastMileFailuresPage.page.locator(".usa-table tbody").waitFor({ state: "visible" });
                });

                test("table has correct headers", async ({ lastMileFailuresPage }) => {
                    await expect(lastMileFailuresPage.page.locator(".column-header-text").nth(0)).toHaveText(
                        /Failed At/,
                    );
                    await expect(lastMileFailuresPage.page.locator(".column-header-text").nth(1)).toHaveText(
                        /ReportId/,
                    );
                    await expect(lastMileFailuresPage.page.locator(".column-header-text").nth(2)).toHaveText(
                        /Receiver/,
                    );
                });

                test("table column 'Failed At' has expected data", async ({ lastMileFailuresPage, isMockDisabled }) => {
                    test.skip(!isMockDisabled, "Mocks are ENABLED, test");
                    const areDatesInRange = await lastMileFailuresPage.tableColumnDateTimeInRange(200);
                    expect(areDatesInRange).toBe(true);
                });
            });

            test("table column 'ReportId' will open a modal with report details", async ({ lastMileFailuresPage }) => {
                const reportId = tableRows(lastMileFailuresPage.page).nth(0).locator("td").nth(1);
                const reportIdCell = await reportId.innerText();
                await reportId.click();

                const modal = lastMileFailuresPage.page.getByTestId("modalWindow").nth(0);
                await expect(modal).toContainText(`Report ID:${reportIdCell}`);
            });

            test.skip("table column 'Receiver' will open receiver edit page", async ({
                lastMileFailuresPage,
                isMockDisabled,
            }) => {
                test.skip(!isMockDisabled, "Mocks are ENABLED, skipping test");
                const receiver = tableRows(lastMileFailuresPage.page).nth(0).locator("td").nth(2);
                const receiverCell = await receiver.getByRole("link").innerText();
                const orgName = receiverCell.slice(0, receiverCell.indexOf("."));
                const receiverName = receiverCell.slice(receiverCell.indexOf(".") + 1);
                await receiver.click();

                await expect(lastMileFailuresPage.page).toHaveURL(
                    `/admin/orgreceiversettings/org/${orgName}/receiver/${receiverName}/action/edit`,
                );
            });
        });
    },
);
