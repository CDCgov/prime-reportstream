import { addDays, endOfDay, startOfDay, subDays } from "date-fns";
import { pageNotFound } from "../../../../../src/content/error/ErrorMessages";
import { AdminReceiverStatusPage } from "../../../../pages/authenticated/admin/receiver-status";
import { test as baseTest, expect, logins } from "../../../../test";

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

test.describe("Admin Receiver Status Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ adminReceiverStatusPage }) => {
            await expect(adminReceiverStatusPage.page).toHaveURL("/login");
        });
    });

    test.describe("authenticated receiver", () => {
        test.use({ storageState: logins.receiver.path });
        test("returns Page Not Found", async ({ adminReceiverStatusPage }) => {
            await expect(adminReceiverStatusPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("authenticated sender", () => {
        test.use({ storageState: logins.sender.path });
        test("returns Page Not Found", async ({ adminReceiverStatusPage }) => {
            await expect(adminReceiverStatusPage.page).toHaveTitle(new RegExp(pageNotFound));
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: logins.admin.path });

        test("If there is an error, the error is shown on the page", async ({ adminReceiverStatusPage }) => {
            adminReceiverStatusPage.mockError = true;
            await adminReceiverStatusPage.reload();

            await expect(adminReceiverStatusPage.page.getByText("there was an error")).toBeVisible();
        });

        test.describe("Header", () => {
            test("has correct title + heading", async ({ adminReceiverStatusPage }) => {
                await adminReceiverStatusPage.testHeader();
            });
        });

        test.describe("When there is no error", () => {
            test.describe("Displays correctly", () => {
                test.describe("filters", () => {
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
                });

                // Failures here indicate potential misalignment of playwright/browser timezone
                test.describe("receiver statuses", () => {
                    test("time periods", async ({ adminReceiverStatusPage }) => {
                        const result = await adminReceiverStatusPage.testReceiverStatusDisplay();
                        expect(result).toBe(true);
                    });
                });
            });

            test.describe("Footer", () => {
                test("has footer and explicit scroll to footer and scroll to top", async ({
                    adminReceiverStatusPage,
                }) => {
                    await adminReceiverStatusPage.testFooter();
                });
            });

            test.describe("Functions correctly", () => {
                test.describe("filters", () => {
                    test.describe("date range", () => {
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

                            await expect(valueDisplay).toHaveText(adminReceiverStatusPage.expectedDateRangeLabelText);
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

                            await expect(valueDisplay).toHaveText(adminReceiverStatusPage.expectedDateRangeLabelText);
                            expect(Object.fromEntries(reqUrl!.searchParams.entries())).toMatchObject({
                                start_date: targetFrom.toISOString(),
                                end_date: targetTo.toISOString(),
                            });
                        });
                    });

                    test("receiver name", async ({ adminReceiverStatusPage }) => {
                        const { organizationName, receiverName, successRate } =
                            adminReceiverStatusPage.timePeriodData[1];

                        const receiversStatusRows = adminReceiverStatusPage.receiverStatusRowsLocator;
                        const expectedReceiverStatusRow = receiversStatusRows.nthCustom(0);
                        const expectedReceiverStatusRowTitle =
                            adminReceiverStatusPage.getExpectedReceiverStatusRowTitle(
                                organizationName,
                                receiverName,
                                successRate,
                            );

                        await expect(receiversStatusRows).toHaveCount(adminReceiverStatusPage.timePeriodData.length);

                        await adminReceiverStatusPage.updateFilters({
                            receiverName,
                        });

                        await expect(receiversStatusRows).toHaveCount(1);
                        await expect(expectedReceiverStatusRow).toBeVisible();
                        await expect(expectedReceiverStatusRow.title).toHaveText(expectedReceiverStatusRowTitle);

                        await adminReceiverStatusPage.resetFilters();

                        await expect(receiversStatusRows).toHaveCount(adminReceiverStatusPage.timePeriodData.length);
                    });

                    test("result message", async ({ adminReceiverStatusPage }) => {
                        const result = await adminReceiverStatusPage.testReceiverMessage();
                        expect(result).toBe(true);
                    });

                    test("success type", async ({ adminReceiverStatusPage }) => {
                        const [failRow, , mixedRow] = adminReceiverStatusPage.timePeriodData;
                        const failRowTitle = adminReceiverStatusPage.getExpectedReceiverStatusRowTitle(
                            failRow.organizationName,
                            failRow.receiverName,
                            failRow.successRate,
                        );
                        const mixedRowTitle = adminReceiverStatusPage.getExpectedReceiverStatusRowTitle(
                            mixedRow.organizationName,
                            mixedRow.receiverName,
                            mixedRow.successRate,
                        );

                        const receiversStatusRows = adminReceiverStatusPage.receiverStatusRowsLocator;
                        const expectedRow = receiversStatusRows.nthCustom(0);

                        await expect(receiversStatusRows).toHaveCount(adminReceiverStatusPage.timePeriodData.length);

                        await adminReceiverStatusPage.updateFilters({
                            successType: "ALL_FAILURE",
                        });
                        await expect(receiversStatusRows).toHaveCount(1);
                        await expect(expectedRow.title).toHaveText(failRowTitle);

                        await adminReceiverStatusPage.updateFilters({
                            successType: "MIXED_SUCCESS",
                        });
                        await expect(receiversStatusRows).toHaveCount(1);
                        await expect(expectedRow.title).toHaveText(mixedRowTitle);

                        await adminReceiverStatusPage.resetFilters();

                        await expect(receiversStatusRows).toHaveCount(4);
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

                    test("time period modals", async ({ adminReceiverStatusPage }) => {
                        const result = await adminReceiverStatusPage.testReceiverTimePeriodModals();
                        expect(result).toBe(true);
                    });

                    test("receiver org links", async ({ adminReceiverStatusPage }) => {
                        const result = await adminReceiverStatusPage.testReceiverOrgLinks();
                        expect(result).toBe(true);
                    });

                    test("receiver links", async ({ adminReceiverStatusPage }) => {
                        const result = await adminReceiverStatusPage.testReceiverLinks();
                        expect(result).toBe(true);
                    });
                });
            });
        });
    });
});
