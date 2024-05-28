import { addDays, endOfDay, format, startOfDay, subDays } from "date-fns";
import { fileURLToPath } from "node:url";
import {
    SUCCESS_RATE_CLASSNAME_MAP,
    SuccessRate,
} from "../../../src/components/Admin/AdminReceiverDashboard.utils";
import { durationFormatShort } from "../../../src/utils/DateTimeUtils";
import { formatDate } from "../../../src/utils/misc";
import {
    AdminReceiverStatusPage,
} from "../../pages/admin/receiver-status";
import { test as baseTest, expect } from "../../test";

const __dirname = fileURLToPath(import.meta.url);

export interface AdminReceiverStatusPageFixtures {
    adminReceiverStatusPage: AdminReceiverStatusPage
}

const test = baseTest.extend<AdminReceiverStatusPageFixtures>({
    adminReceiverStatusPage: async (args, use) => {
        const page = new AdminReceiverStatusPage(args)
        await page.goto();
        await use(page);
    }
})

test.describe("Admin Receiver Status Page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("authenticated receiver", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });
        test("returns Page Not Found", async ({ page }) => {
            await expect(page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated sender", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });
        test("returns Page Not Found", async ({ page }) => {
            await expect(page).toHaveTitle(/Page Not Found/);
        });
    });

    test.describe("authenticated admin", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test("If there is an error, the error is shown on the page", async ({
            page,
        }) => {
            await expect(page.getByText("there was an error")).toBeVisible();
        });

        test("Has correct title", async ({ adminReceiverStatusPage }) => {
            await expect(adminReceiverStatusPage.page).toHaveURL(
                adminReceiverStatusPage.url,
            );
            await expect(adminReceiverStatusPage.page).toHaveTitle(
                adminReceiverStatusPage.title,
            );
        });

        test.describe("When there is no error", () => {
            test.describe("Displays correctly", () => {
                test("header", async ({ adminReceiverStatusPage }) => {
                    await expect(
                        adminReceiverStatusPage.heading,
                    ).toBeVisible();
                });

                test.describe("filters", () => {
                    test("date range", async ({adminReceiverStatusPage}) => {
                        const {button, expectedTooltipText, label, modal, modalEndButton, modalEndInput, modalStartButton, modalStartInput, tooltip} = adminReceiverStatusPage.filterFormInputs.dateRange;
                        await expect(label).toBeVisible();
                        await expect(
                            button,
                        ).toBeVisible();

                        await expect(tooltip).toBeHidden();
                        await tooltip.hover();
                        await expect(tooltip).toBeVisible();
                        await expect(tooltip).toHaveText(expectedTooltipText);
                    })
                    test("receiver name", async ({ adminReceiverStatusPage }) => {
                        const {
                            input,
                            expectedTooltipText,
                            label,
                            tooltip,
                            expectedInputDefaultValue
                        } = adminReceiverStatusPage.filterFormInputs.receiverName;
                        await expect(label).toBeVisible();
                        await expect(input).toBeVisible();
                        await expect(input).toHaveValue(
                            expectedInputDefaultValue,
                        );

                        await expect(tooltip).toBeHidden();
                        await tooltip.hover();
                        await expect(tooltip).toBeVisible();
                        await expect(tooltip).toHaveText(expectedTooltipText);
                    });
                    test("results message", async ({ adminReceiverStatusPage }) => {
                        const { input, expectedTooltipText, label, tooltip, expectedInputDefaultValue } =
                            adminReceiverStatusPage.filterFormInputs
                                .resultMessage;
                        await expect(label).toBeVisible();
                        await expect(input).toBeVisible();
                        await expect(input).toHaveValue(
                            expectedInputDefaultValue,
                        );

                        await expect(tooltip).toBeHidden();
                        await tooltip.hover();
                        await expect(tooltip).toBeVisible();
                        await expect(tooltip).toHaveText(expectedTooltipText);
                    });
                    test("success type", async ({ adminReceiverStatusPage }) => {
                        const { input, expectedTooltipText, label, tooltip, expectedInputDefaultValue } =
                            adminReceiverStatusPage.filterFormInputs
                                .successType;
                        await expect(label).toBeVisible();
                        await expect(input).toBeVisible();
                        await expect(input).toHaveValue(
                            expectedInputDefaultValue,
                        );

                        await expect(tooltip).toBeHidden();
                        await tooltip.hover();
                        await expect(tooltip).toBeVisible();
                        await expect(tooltip).toHaveText(expectedTooltipText);
                    });

                    // Failures here indicate potential misalignment of playwright/browser timezone
                    test.describe("receiver statuses", () => {
                        test("time periods", async ({ page }) => {
                            const {
                                expectedParsedTimePeriodData,
                                mockGetReceiverStatus,
                                range: [startDate, endDate],
                            } = await goto(page);

                            const statusRows =
                                getReceiverStatusRowsLocator(page);
                            await expect(statusRows).toHaveCount(
                                new Set(
                                    mockGetReceiverStatus.map(
                                        (r) => r.receiverId,
                                    ),
                                ).size,
                            );

                            const expectedDaysText = [
                                startDate.toLocaleDateString(),
                                subDays(endDate, 1).toLocaleDateString(),
                                endDate.toLocaleDateString(),
                            ].join("            ");
                            for (const [
                                i,
                                {
                                    days,
                                    successRate,
                                    organizationName,
                                    receiverName,
                                    successRateType,
                                },
                            ] of expectedParsedTimePeriodData.entries()) {
                                const {
                                    title,
                                    display,
                                    days: daysLoc,
                                } = statusRows.nthWithCustomLocators(i);

                                const expectedTitleText =
                                    getExpectedReceiverStatusRowTitle(
                                        organizationName,
                                        receiverName,
                                        successRate,
                                    );
                                const expectedClass = new RegExp(
                                    SUCCESS_RATE_CLASSNAME_MAP[successRateType],
                                );

                                await expect(title).toBeVisible();
                                await expect(title).toHaveText(
                                    expectedTitleText,
                                );
                                await expect(title).toHaveClass(expectedClass);

                                await expect(display).toBeVisible();
                                await expect(display).toHaveText(
                                    expectedDaysText,
                                );

                                await expect(daysLoc).toHaveCount(days.length);

                                for (const [
                                    i,
                                    { timePeriods },
                                ] of days.entries()) {
                                    const daySlices =
                                        daysLoc.nthWithCustomLocators(
                                            i,
                                        ).timePeriods;
                                    await expect(daySlices).toHaveCount(
                                        timePeriods.length,
                                    );

                                    for (const [
                                        i,
                                        { successRateType },
                                    ] of timePeriods.entries()) {
                                        const sliceEle = daySlices.nth(i);
                                        const expectedClass = new RegExp(
                                            SUCCESS_RATE_CLASSNAME_MAP[
                                                successRateType
                                            ],
                                        );

                                        await expect(sliceEle).toBeVisible();
                                        await expect(sliceEle).toHaveClass(
                                            expectedClass,
                                        );
                                    }
                                }
                            }
                        });
                    });
                });
            });

            test.describe("Functions correctly", () => {
                test.describe("filters", () => {
                    test.describe("date range", () => {
                        test("works through calendar", async ({ adminReceiverStatusPage }) => {
                            const {label } = adminReceiverStatusPage.filterFormInputs.dateRange;
                            const now = new Date();
                            const targetFrom = startOfDay(subDays(now, 3));
                            const targetTo = addDays(endOfDay(now), 1);

                            const reqUrl = await adminReceiverStatusPage.updateFilterDateRange(targetFrom, targetTo, true, "calendar")

                            await expect(label).toHaveText(
                                adminReceiverStatusPage.expectedDateRangeLabelText,
                            );
                            expect(
                                Object.fromEntries(
                                    reqUrl.searchParams.entries(),
                                ),
                            ).toMatchObject({
                                start_date: targetFrom.toISOString(),
                                end_date: targetTo.toISOString(),
                            });
                        });

                        test("works through textboxes", async ({ adminReceiverStatusPage }) => {
                            const {label} = adminReceiverStatusPage.filterFormInputs.dateRange;
                            await expect(
                                adminReceiverStatusPage.receiverStatusRowsLocator,
                            ).not.toHaveCount(0);
                            const now = new Date();
                            const targetFrom = startOfDay(subDays(now, 3));
                            const targetTo = addDays(endOfDay(now), 1);

                            const reqUrl =
                                await adminReceiverStatusPage.updateFilterDateRange(
                                    targetFrom,
                                    targetTo,
                                );
                                
                            await expect(label).toHaveText(adminReceiverStatusPage.expectedDateRangeLabelText);
                            expect(
                                Object.fromEntries(
                                    reqUrl.searchParams.entries(),
                                ),
                            ).toMatchObject({
                                start_date: targetFrom.toISOString(),
                                end_date: targetTo.toISOString(),
                            });
                        });
                    });

                    test("receiver name", async ({ adminReceiverStatusPage }) => {
                        const { organizationName, receiverName, successRate } =
                        adminReceiverStatusPage.timePeriodData[1];

                        const receiversStatusRows =
                            getReceiverStatusRowsLocator(page);
                        const expectedReceiverStatusRow =
                            receiversStatusRows.nthWithCustomLocators(0);
                        const expectedReceiverStatusRowTitle =
                            getExpectedReceiverStatusRowTitle(
                                organizationName,
                                receiverName,
                                successRate,
                            );
                        const container =
                            getFilterOptionContainersLocator(page).nth(1);
                        const input = container.locator("input");

                        await expect(receiversStatusRows).toHaveCount(
                            expectedParsedTimePeriodData.length,
                        );

                        await expect(input).toBeVisible();

                        await input.fill(receiverName);
                        await expect(receiversStatusRows).toHaveCount(1);
                        await expect(expectedReceiverStatusRow).toBeVisible();
                        await expect(
                            expectedReceiverStatusRow.title,
                        ).toHaveText(expectedReceiverStatusRowTitle);

                        await input.clear();
                        await expect(receiversStatusRows).toHaveCount(
                            expectedParsedTimePeriodData.length,
                        );
                    });

                    test("result message", async ({ page }) => {
                        const { expectedParsedTimePeriodData } =
                            await goto(page);

                        // get first entry's result from all-fail receiver's first day -> third time period
                        const receiverI = 1;
                        const dayI = 0;
                        const timePeriodI = 2;
                        const entryI = 0;
                        const { days } =
                            expectedParsedTimePeriodData[receiverI];
                        const { connectionCheckResult } =
                            days[dayI].timePeriods[timePeriodI].entries[entryI];

                        const receiversStatusRows =
                            getReceiverStatusRowsLocator(page);
                        const container =
                            getFilterOptionContainersLocator(page).nth(2);
                        const input = container.locator("input");

                        await expect(receiversStatusRows).toHaveCount(
                            expectedParsedTimePeriodData.length,
                        );

                        await expect(input).toBeVisible();

                        await input.fill(connectionCheckResult);

                        for (const [
                            i,
                            { days },
                        ] of expectedParsedTimePeriodData.entries()) {
                            const isRowExpected = i === receiverI;
                            const row =
                                getReceiverStatusRowsLocator(
                                    page,
                                ).nthWithCustomLocators(i);

                            for (const [i, { timePeriods }] of days.entries()) {
                                const isDayExpected =
                                    isRowExpected && i === dayI;
                                const rowDay =
                                    row.days.nthWithCustomLocators(i);

                                for (const [i] of timePeriods.entries()) {
                                    const isTimePeriodExpected =
                                        isDayExpected && i === timePeriodI;
                                    const expectedClass = !isTimePeriodExpected
                                        ? /success-result-hidden/
                                        : /^((?!success-result-hidden).)*$/;
                                    const rowDayTimePeriod =
                                        rowDay.timePeriods.nth(i);

                                    await expect(
                                        rowDayTimePeriod,
                                    ).toBeVisible();
                                    await expect(rowDayTimePeriod).toHaveClass(
                                        expectedClass,
                                    );
                                }
                            }
                        }
                    });

                    test("success type", async ({ page }) => {
                        const { expectedParsedTimePeriodData } =
                            await goto(page);
                        const [, failRow, mixedRow] =
                            expectedParsedTimePeriodData;
                        const failRowTitle = getExpectedReceiverStatusRowTitle(
                            failRow.organizationName,
                            failRow.receiverName,
                            failRow.successRate,
                        );
                        const mixedRowTitle = getExpectedReceiverStatusRowTitle(
                            mixedRow.organizationName,
                            mixedRow.receiverName,
                            mixedRow.successRate,
                        );

                        const receiversStatusRows =
                            getReceiverStatusRowsLocator(page);
                        const expectedRow =
                            receiversStatusRows.nthWithCustomLocators(0);
                        const container =
                            getFilterOptionContainersLocator(page).nth(3);
                        const input = container.locator("select");

                        await expect(receiversStatusRows).toHaveCount(
                            expectedParsedTimePeriodData.length,
                        );

                        await expect(input).toBeVisible();

                        await input.selectOption("ALL_FAILURE");
                        await expect(receiversStatusRows).toHaveCount(1);
                        await expect(expectedRow.title).toHaveText(
                            failRowTitle,
                        );

                        await input.selectOption("MIXED_SUCCESS");
                        await expect(receiversStatusRows).toHaveCount(1);
                        await expect(expectedRow.title).toHaveText(
                            mixedRowTitle,
                        );

                        await input.selectOption("UNDEFINED");
                        await expect(receiversStatusRows).toHaveCount(4);
                    });
                });

                test.describe("receiver statuses", () => {
                    test.describe("date range length changes", () => {
                        test("increases", async ({ page }) => {
                            const rows = getReceiverStatusRowsLocator(page);
                            const days = rows.nthWithCustomLocators(0).days;
                            await expect(rows).not.toHaveCount(0);
                            const now = new Date();
                            const targetFrom = startOfDay(subDays(now, 3));
                            const targetTo = endOfDay(now);
                            await updateFilterDateRangeByText(
                                page,
                                targetFrom,
                                targetTo,
                            );
                            await expect(days).toHaveCount(4);
                        });

                        test("decreases", async ({ page }) => {
                            const rows = getReceiverStatusRowsLocator(page);
                            const days = rows.nthWithCustomLocators(0).days;
                            await expect(rows).not.toHaveCount(0);
                            const now = new Date();
                            const targetFrom = startOfDay(subDays(now, 1));
                            const targetTo = endOfDay(now);
                            await updateFilterDateRangeByText(
                                page,
                                targetFrom,
                                targetTo,
                            );
                            await expect(days).toHaveCount(2);
                        });
                    });

                    test("time period modals", async ({ page }) => {
                        const { expectedParsedTimePeriodData } =
                            await goto(page);

                        for (const [
                            i,
                            { days },
                        ] of expectedParsedTimePeriodData.entries()) {
                            const { days: daysLoc } =
                                getReceiverStatusRowsLocator(
                                    page,
                                ).nthWithCustomLocators(i);

                            for (const [dayI, day] of days.entries()) {
                                for (const [
                                    i,
                                    { successRateType, entries },
                                ] of day.timePeriods.entries()) {
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
                                    const modal = page.getByRole("dialog");
                                    const overlay =
                                        modal.locator(".usa-modal-overlay");
                                    const sliceEle = daysLoc
                                        .nthWithCustomLocators(dayI)
                                        .timePeriods.nth(i);

                                    const isModalExpectedVisible =
                                        successRateType !==
                                        SuccessRate.UNDEFINED;

                                    await sliceEle.click({ force: true });
                                    await expect(overlay).toBeAttached({
                                        attached: isModalExpectedVisible,
                                    });

                                    if (isModalExpectedVisible) {
                                        const expectedResultText =
                                            connectionCheckSuccessful
                                                ? "success"
                                                : "failed";
                                        const expectedModalText = `Results for connection verification checkOrg:${organizationName} (id: ${organizationId})Receiver:${receiverName} (id: ${receiverId})Result:${expectedResultText}Started At:${formatDate(connectionCheckStartedAt)}${connectionCheckStartedAt.toISOString()}Time to complete:${durationFormatShort(connectionCheckCompletedAt, connectionCheckStartedAt)}Result message:${connectionCheckResult}`;

                                        await expect(overlay).toBeVisible();
                                        await expect(overlay).toHaveText(
                                            expectedModalText,
                                        );

                                        await overlay.press("Escape");
                                    }
                                }
                            }
                        }
                    });

                    test("receiver org links", async ({ page }) => {
                        const rows = getReceiverStatusRowsLocator(page);

                        for (const [
                            i,
                            { organizationName },
                        ] of expectedParsedTimePeriodData.entries()) {
                            const row = rows.nthWithCustomLocators(i);

                            const link = row.title.getByRole("link", {
                                name: organizationName,
                            });
                            await expect(link).toBeVisible();
                            await link.click();
                            await expect(page).toHaveURL(
                                `/admin/orgsettings/org/${organizationName}`,
                            );
                            await page.goBack();
                        }
                    });

                    test("receiver links", async ({ page }) => {
                        const rows = getReceiverStatusRowsLocator(page);

                        for (const [
                            i,
                            { receiverName, organizationName },
                        ] of expectedParsedTimePeriodData.entries()) {
                            const row = rows.nthWithCustomLocators(i);

                            const link = row.title.getByRole("link", {
                                name: receiverName,
                            });
                            await expect(link).toBeVisible();
                            await link.click();
                            await expect(page).toHaveURL(
                                `/admin/orgreceiversettings/org/${organizationName}/receiver/${receiverName}/action/edit`,
                            );
                            await page.goBack();
                        }
                    });
                });
            });
        });
    });
});
