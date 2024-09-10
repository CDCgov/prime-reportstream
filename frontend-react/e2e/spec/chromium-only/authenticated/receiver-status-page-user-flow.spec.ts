import { addDays, endOfDay, startOfDay, subDays } from "date-fns";
import { AdminReceiverStatusPage } from "../../../pages/authenticated/admin/receiver-status";
import { test as baseTest, expect, logins } from "../../../test";

export interface AdminReceiverStatusPageFixtures {
    adminReceiverStatusPage: AdminReceiverStatusPage;
}

const test = baseTest.extend<AdminReceiverStatusPageFixtures>({
    adminReceiverStatusPage: async (
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
        const page = new AdminReceiverStatusPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            isFrontendWarningsLog,
            frontendWarningsLogPath,
            isTestOrg: true,
        });
        await page.goto();
        await use(page);
    },
});

test.describe("Admin Receiver Status Page",
    {
        // TODO: Investigate Admin Receiver Status Page › functions correctly › receiver statuses › time period modals
        // tag: "@smoke",
    }, () => {
        test.use({ storageState: logins.admin.path });
        test.describe("displays correctly", () => {
            test.describe("header", () => {
                test(
                    "has correct title + heading",
                    async ({ adminReceiverStatusPage }) => {
                        await adminReceiverStatusPage.testHeader();
                    },
                );
            });

            test.describe(
                "filters",
                () => {
                    test("date range", async ({ adminReceiverStatusPage }) => {
                        const { button, label, modalOverlay, valueDisplay } =
                            adminReceiverStatusPage.filterFormInputs.dateRange;
                        await expect(label).toBeVisible();
                        await expect(button).toBeVisible();
                        await expect(valueDisplay).toHaveText(adminReceiverStatusPage.expectedDateRangeLabelText);
                        await expect(modalOverlay).toBeHidden();
                    });

                    test("receiver name", async ({ adminReceiverStatusPage }) => {
                        const { input, expectedTooltipText, label, tooltip, expectedDefaultValue } =
                            adminReceiverStatusPage.filterFormInputs.receiverName;
                        await expect(label).toBeVisible();
                        await expect(input).toBeVisible();
                        await expect(input).toHaveValue(expectedDefaultValue);

                        await expect(tooltip).toBeHidden();
                        await input.hover();
                        await expect(tooltip).toBeVisible();
                        await expect(tooltip).toHaveText(expectedTooltipText);
                    });

                    test("results message", async ({ adminReceiverStatusPage }) => {
                        const { input, expectedTooltipText, label, tooltip, expectedDefaultValue } =
                            adminReceiverStatusPage.filterFormInputs.resultMessage;
                        await expect(label).toBeVisible();
                        await expect(input).toBeVisible();
                        await expect(input).toHaveValue(expectedDefaultValue);

                        await expect(tooltip).toBeHidden();
                        await input.hover();
                        await expect(tooltip).toBeVisible();
                        await expect(tooltip).toHaveText(expectedTooltipText);
                    });

                    test("success type", async ({ adminReceiverStatusPage }) => {
                        const { input, expectedTooltipText, label, tooltip, expectedDefaultValue } =
                            adminReceiverStatusPage.filterFormInputs.successType;
                        await expect(label).toBeVisible();
                        await expect(input).toBeVisible();
                        await expect(input).toHaveValue(expectedDefaultValue);

                        await expect(tooltip).toBeHidden();
                        await input.hover();
                        await expect(tooltip).toBeVisible();
                        await expect(tooltip).toHaveText(expectedTooltipText);
                    });
                },
            );

            // Failures here indicate potential misalignment of playwright/browser timezone
            test.describe("receiver statuses", () => {
                test("time periods", async ({ adminReceiverStatusPage }) => {
                    const result = await adminReceiverStatusPage.testReceiverStatusDisplay(true);
                    expect(result).toBe(true);
                });
            });

            test.describe("has footer", () => {
                test("has footer and explicit scroll to footer and scroll to top",
                    async ({
                               adminReceiverStatusPage,
                           }) => {
                        await adminReceiverStatusPage.testFooter();
                    });
            });
        });

        test.describe("functions correctly", () => {
            test.describe("filters", () => {
                test.describe(
                    "date range",
                    () => {
                        test("works through calendar", async ({ adminReceiverStatusPage }) => {
                            const { valueDisplay } = adminReceiverStatusPage.filterFormInputs.dateRange;
                            const now = new Date();
                            const targetFrom = startOfDay(subDays(now, 3));
                            const targetTo = addDays(endOfDay(now), 1);

                            const reqUrl = await adminReceiverStatusPage.updateFilters({
                                dateRange: {
                                    value: [targetFrom, targetTo],
                                },
                            });
                            expect(reqUrl).toBeDefined();

                            await expect(valueDisplay).toHaveText(
                                adminReceiverStatusPage.expectedDateRangeLabelText,
                            );
                            expect(Object.fromEntries(reqUrl!.searchParams.entries())).toMatchObject({
                                start_date: targetFrom.toISOString(),
                                end_date: targetTo.toISOString(),
                            });
                        });

                        test("works through textboxes", async ({ adminReceiverStatusPage }) => {
                            const { valueDisplay } = adminReceiverStatusPage.filterFormInputs.dateRange;
                            await expect(adminReceiverStatusPage.receiverStatusRowsLocator).not.toHaveCount(0);
                            const now = new Date();
                            const targetFrom = startOfDay(subDays(now, 3));
                            const targetTo = addDays(endOfDay(now), 1);

                            const reqUrl = await adminReceiverStatusPage.updateFilters({
                                dateRange: {
                                    value: [targetFrom, targetTo],
                                },
                            });

                            expect(reqUrl).toBeDefined();

                            await expect(valueDisplay).toHaveText(
                                adminReceiverStatusPage.expectedDateRangeLabelText,
                            );
                            expect(Object.fromEntries(reqUrl!.searchParams.entries())).toMatchObject({
                                start_date: targetFrom.toISOString(),
                                end_date: targetTo.toISOString(),
                            });
                        });
                    },
                );

                // TODO: revisit after filters have been fixed per ticket #15737
                test.skip("receiver name", async ({adminReceiverStatusPage, isMockDisabled}) => {
                    test.skip(!isMockDisabled, "Mocks are ENABLED, skipping 'receiver name' test");
                    const {organizationName, receiverName, successRate} =
                        adminReceiverStatusPage.timePeriodData[1];

                    const receiversStatusRows = adminReceiverStatusPage.receiverStatusRowsLocator;
                    const defaultReceiversStatusRowsCount = await receiversStatusRows.count();
                    const expectedReceiverStatusRow = receiversStatusRows.nthCustom(0);
                    const expectedReceiverStatusRowTitle =
                        adminReceiverStatusPage.getExpectedReceiverStatusRowTitle(
                            organizationName,
                            receiverName,
                            successRate,
                        );

                    expect(defaultReceiversStatusRowsCount).toBe(adminReceiverStatusPage.timePeriodData.length);

                    await adminReceiverStatusPage.updateFilters({
                        receiverName,
                    });

                    const receiversStatusRowsCount = await receiversStatusRows.count();
                    expect(receiversStatusRowsCount).toBeGreaterThanOrEqual(1);
                    await expect(expectedReceiverStatusRow).toBeVisible();
                    await expect(expectedReceiverStatusRow.title).toHaveText(expectedReceiverStatusRowTitle);

                    await adminReceiverStatusPage.resetFilters();

                    expect(defaultReceiversStatusRowsCount).toBe(adminReceiverStatusPage.timePeriodData.length);
                });

                test.skip("result message", async ({adminReceiverStatusPage, isMockDisabled}) => {
                    test.skip(!isMockDisabled, "Mocks are ENABLED, skipping 'result message' test");
                    // get first entry's result from all-fail receiver's first day -> third time period
                    const receiverI = 0;
                    const dayI = 0;
                    const timePeriodI = 2;
                    const entryI = 0;
                    const {days} = adminReceiverStatusPage.timePeriodData[receiverI];
                    const {connectionCheckResult} = days[dayI].timePeriods[timePeriodI].entries[entryI];

                    const receiversStatusRows = adminReceiverStatusPage.receiverStatusRowsLocator;

                    await adminReceiverStatusPage.updateFilters({
                        resultMessage: connectionCheckResult,
                    });

                    for (const [i, {days}] of adminReceiverStatusPage.timePeriodData.entries()) {
                        const row = receiversStatusRows.nthCustom(i);

                        for (const [i, {timePeriods}] of days.entries()) {
                            const rowDay = row.days.nthCustom(i);

                            for (const [i] of timePeriods.entries()) {
                                const rowDayTimePeriod = rowDay.timePeriods.nth(i);

                                await expect(rowDayTimePeriod).toHaveClass(/success-result-hidden/);
                            }
                        }
                    }

                    await adminReceiverStatusPage.resetFilters();

                    // TODO: revisit after filters have been fixed per ticket #15737
                    // await adminReceiverStatusPage.testReceiverStatusDisplay();
                });

                test.skip("success type", async ({ adminReceiverStatusPage, isMockDisabled }) => {
                    test.skip(!isMockDisabled, "Mocks are ENABLED, skipping 'success type' test");
                    const [failRow, ,] = adminReceiverStatusPage.timePeriodData;
                    const failRowTitle = adminReceiverStatusPage.getExpectedReceiverStatusRowTitle(
                        failRow.organizationName,
                        failRow.receiverName,
                        failRow.successRate,
                    );
                    // const mixedRowTitle = adminReceiverStatusPage.getExpectedReceiverStatusRowTitle(
                    //     mixedRow.organizationName,
                    //     mixedRow.receiverName,
                    //     mixedRow.successRate,
                    // );

                    const receiversStatusRows = adminReceiverStatusPage.receiverStatusRowsLocator;
                    const defaultReceiversStatusRowsCount = await receiversStatusRows.count();
                    const expectedRow = receiversStatusRows.nthCustom(0);

                    expect(defaultReceiversStatusRowsCount).toBe(adminReceiverStatusPage.timePeriodData.length);

                    await adminReceiverStatusPage.updateFilters({
                        successType: "ALL_FAILURE",
                    });
                    let receiversStatusRowsCount = await receiversStatusRows.count();

                    expect(receiversStatusRowsCount).toBeGreaterThanOrEqual(1);
                    await expect(expectedRow.title).toHaveText(failRowTitle);

                    await adminReceiverStatusPage.updateFilters({
                        successType: "MIXED_SUCCESS",
                    });
                    receiversStatusRowsCount = await receiversStatusRows.count();
                    expect(receiversStatusRowsCount).toBeGreaterThanOrEqual(1);
                    // TODO: revisit after filters have been fixed per ticket #15737
                    // await expect(expectedRow.title).toHaveText(mixedRowTitle);

                    // await adminReceiverStatusPage.resetFilters();
                    // receiversStatusRowsCount = await receiversStatusRows.count();
                    //
                    // expect(receiversStatusRowsCount).toBe(defaultReceiversStatusRowsCount);
                });
            });

            test.describe("receiver statuses", () => {
                test.describe("date range length changes", () => {
                    test("increases", async ({ adminReceiverStatusPage }) => {
                        const rows = adminReceiverStatusPage.receiverStatusRowsLocator;
                        const days = rows.nthCustom(0).days;
                        await expect(rows).not.toHaveCount(0);
                        const now = new Date();
                        const targetFrom = startOfDay(subDays(now, 3));
                        const targetTo = endOfDay(now);
                        await adminReceiverStatusPage.updateFilters({
                            dateRange: {
                                value: [targetFrom, targetTo],
                            },
                        });
                        await expect(days).toHaveCount(4);
                    });

                    test("decreases", async ({ adminReceiverStatusPage }) => {
                        const rows = adminReceiverStatusPage.receiverStatusRowsLocator;
                        const days = rows.nthCustom(0).days;
                        await expect(rows).not.toHaveCount(0);
                        const now = new Date();
                        const targetFrom = startOfDay(subDays(now, 1));
                        const targetTo = endOfDay(now);
                        await adminReceiverStatusPage.updateFilters({
                            dateRange: {
                                value: [targetFrom, targetTo],
                            },
                        });
                        await expect(days).toHaveCount(2);
                    });
                });

                test.skip("time period modals", async ({ adminReceiverStatusPage }) => {
                    const result = await adminReceiverStatusPage.testReceiverTimePeriodModals(true);
                    expect(result).toBe(true);
                });

                test("receiver org links", async ({ adminReceiverStatusPage }) => {
                    const result = await adminReceiverStatusPage.testReceiverOrgLinks(true);
                    expect(result).toBe(true);
                });

                test("receiver links", async ({ adminReceiverStatusPage }) => {
                    const result = await adminReceiverStatusPage.testReceiverLinks(true);
                    expect(result).toBe(true);
                });
            });
        });
    });
