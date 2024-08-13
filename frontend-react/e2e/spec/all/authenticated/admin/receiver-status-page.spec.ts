import { addDays, endOfDay, startOfDay, subDays } from "date-fns";
import type { RSOrganizationSettings } from "../../../../../src/config/endpoints/settings";
import { SuccessRate } from "../../../../../src/pages/admin/receiver-dashboard/utils";
import { durationFormatShort } from "../../../../../src/utils/DateTimeUtils";
import { formatDate } from "../../../../../src/utils/misc";
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
            await expect(adminReceiverStatusPage.page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated sender", () => {
        test.use({ storageState: logins.sender.path });
        test("returns Page Not Found", async ({ adminReceiverStatusPage }) => {
            await expect(adminReceiverStatusPage.page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: logins.admin.path });

        test("If there is an error, the error is shown on the page", async ({ adminReceiverStatusPage }) => {
            adminReceiverStatusPage.mockError = true;
            await adminReceiverStatusPage.reload();

            await expect(adminReceiverStatusPage.page.getByText("there was an error")).toBeVisible();
        });

        test(
            "Has correct title",
            {
                tag: "@smoke",
            },
            async ({ adminReceiverStatusPage }) => {
                await expect(adminReceiverStatusPage.page).toHaveURL(adminReceiverStatusPage.url);
                await expect(adminReceiverStatusPage.page).toHaveTitle(adminReceiverStatusPage.title);
            },
        );

        test.describe("When there is no error", () => {
            test.describe("Displays correctly", () => {
                test("header", async ({ adminReceiverStatusPage }) => {
                    await expect(adminReceiverStatusPage.heading).toBeVisible();
                });

                test.describe(
                    "filters",
                    {
                        tag: "@smoke",
                    },
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
                        const result = await adminReceiverStatusPage.testReceiverStatusDisplay();
                        expect(result).toBe(true);
                    });
                });
            });

            test.describe("Functions correctly", () => {
                test.describe("filters", () => {
                    test.describe(
                        "date range",
                        {
                            tag: "@smoke",
                        },
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
                        // get first entry's result from all-fail receiver's first day -> third time period
                        const receiverI = 0;
                        const dayI = 0;
                        const timePeriodI = 2;
                        const entryI = 0;
                        const { days } = adminReceiverStatusPage.timePeriodData[receiverI];
                        const { connectionCheckResult } = days[dayI].timePeriods[timePeriodI].entries[entryI];

                        const receiversStatusRows = adminReceiverStatusPage.receiverStatusRowsLocator;

                        await adminReceiverStatusPage.updateFilters({
                            resultMessage: connectionCheckResult,
                        });

                        for (const [i, { days }] of adminReceiverStatusPage.timePeriodData.entries()) {
                            const isRowExpected = i === receiverI;
                            const row = receiversStatusRows.nthCustom(i);

                            for (const [i, { timePeriods }] of days.entries()) {
                                const isDayExpected = isRowExpected && i === dayI;
                                const rowDay = row.days.nthCustom(i);

                                for (const [i] of timePeriods.entries()) {
                                    const isTimePeriodExpected = isDayExpected && i === timePeriodI;
                                    const expectedClass = !isTimePeriodExpected
                                        ? /success-result-hidden/
                                        : /^((?!success-result-hidden).)*$/;
                                    const rowDayTimePeriod = rowDay.timePeriods.nth(i);

                                    await expect(rowDayTimePeriod).toBeVisible();
                                    await expect(rowDayTimePeriod).toHaveClass(expectedClass);
                                }
                            }
                        }

                        await adminReceiverStatusPage.resetFilters();

                        await adminReceiverStatusPage.testReceiverStatusDisplay();
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
                        const overlay = adminReceiverStatusPage.filterFormInputs.dateRange.modalOverlay;
                        for (const [i, { days }] of adminReceiverStatusPage.timePeriodData.entries()) {
                            const { days: daysLoc } = adminReceiverStatusPage.receiverStatusRowsLocator.nthCustom(i);

                            for (const [dayI, day] of days.entries()) {
                                for (const [i, { successRateType, entries }] of day.timePeriods.entries()) {
                                    // only first entry in time period is currently displayed
                                    const {
                                        organizationName,
                                        organizationId,
                                        receiverId,
                                        receiverName,
                                        connectionCheckSuccessful,
                                        connectionCheckStartedAt,
                                        connectionCheckCompletedAt,
                                        connectionCheckResult,
                                    } = entries[0] ?? {};
                                    const sliceEle = daysLoc.nthCustom(dayI).timePeriods.nth(i);

                                    const isModalExpectedVisible = successRateType !== SuccessRate.UNDEFINED;

                                    await sliceEle.click({ force: true });
                                    await expect(overlay).toBeAttached({
                                        attached: isModalExpectedVisible,
                                    });

                                    if (isModalExpectedVisible) {
                                        const expectedResultText = connectionCheckSuccessful ? "success" : "failed";
                                        const expectedModalText = `Results for connection verification checkOrg:${organizationName} (id: ${organizationId})Receiver:${receiverName} (id: ${receiverId})Result:${expectedResultText}Started At:${formatDate(connectionCheckStartedAt)}${connectionCheckStartedAt.toISOString()}Time to complete:${durationFormatShort(connectionCheckCompletedAt, connectionCheckStartedAt)}Result message:${connectionCheckResult}`;

                                        await expect(overlay).toBeVisible();
                                        await expect(overlay).toHaveText(expectedModalText);

                                        await overlay.press("Escape");
                                    }
                                }
                            }
                        }
                    });

                    test("receiver org links", async ({ adminReceiverStatusPage }) => {
                        const rows = adminReceiverStatusPage.receiverStatusRowsLocator;

                        for (const [i, { organizationName }] of adminReceiverStatusPage.timePeriodData.entries()) {
                            const row = rows.nthCustom(i);

                            const link = row.title.getByRole("link", {
                                name: organizationName,
                            });
                            const expectedUrl = adminReceiverStatusPage.getExpectedStatusOrganizationUrl(i);
                            await expect(link).toBeVisible();
                            const p = adminReceiverStatusPage.page.route(
                                `api/settings/organizations/${organizationName}`,
                                (route) =>
                                    route.fulfill({
                                        json: {
                                            description: "fake",
                                            filters: [],
                                            name: organizationName,
                                            jurisdiction: "fake",
                                            version: 0,
                                            createdAt: "",
                                            createdBy: "",
                                        } satisfies RSOrganizationSettings,
                                    }),
                            );
                            await link.click();
                            await expect(adminReceiverStatusPage.page).toHaveURL(expectedUrl);
                            await p;
                            await adminReceiverStatusPage.page.goBack();
                        }
                    });

                    test("receiver links", async ({ adminReceiverStatusPage }) => {
                        const rows = adminReceiverStatusPage.receiverStatusRowsLocator;

                        for (const [i, { receiverName }] of adminReceiverStatusPage.timePeriodData.entries()) {
                            const row = rows.nthCustom(i);

                            const link = row.title.getByRole("link", {
                                name: receiverName,
                            });
                            await expect(link).toBeVisible();
                            await link.click();
                            await expect(adminReceiverStatusPage.page).toHaveURL(
                                adminReceiverStatusPage.getExpectedStatusReceiverUrl(i),
                            );
                            await adminReceiverStatusPage.page.goBack();
                        }
                    });
                });
            });
        });
    });
});
