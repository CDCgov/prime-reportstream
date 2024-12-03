import { expect, Locator } from "@playwright/test";
import { endOfDay, format, startOfDay, subDays } from "date-fns";
import type { RSOrganizationSettings } from "../../../../src/config/endpoints/settings";
import { RSReceiverStatus } from "../../../../src/hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus";
import {
    createStatusTimePeriodData,
    isConnectionResultMatch,
    MATCHING_FILTER_CLASSNAME_MAP,
    MatchingFilter,
    SUCCESS_RATE_CLASSNAME_MAP,
    SuccessRate,
} from "../../../../src/pages/admin/receiver-dashboard/utils";
import { DatePair, dateShortFormat, durationFormatShort } from "../../../../src/utils/DateTimeUtils";
import { formatDate } from "../../../../src/utils/misc";
import { createMockGetReceiverStatus } from "../../../mocks/receiverStatus";
import { BasePage, BasePageTestArgs, type ResponseHandlerEntry, type RouteHandlerFulfillEntry } from "../../BasePage";

export interface AdminReceiverStatusPageUpdateFiltersProps {
    dateRange?: {
        value: DatePair;
        inputMethod?: "textbox" | "calendar";
        isRequestAwaited?: boolean;
    };
    receiverName?: string;
    resultMessage?: string;
    successType?: string;
}

export class AdminReceiverStatusPage extends BasePage {
    static readonly API_RECEIVER_STATUS = "/api/adm/listreceiversconnstatus?*";
    protected _receiverStatus: RSReceiverStatus[];
    protected _timePeriodData: ReturnType<typeof createStatusTimePeriodData>;

    readonly filterForm: Locator;
    readonly filterFormInputs: {
        dateRange: {
            label: Locator;
            button: Locator;
            modalOverlay: Locator;
            expectedModalOverlayText: string;
            modalOverlayCalendar: Locator;
            modalPrimaryButton: Locator;
            modalStartInput: Locator;
            modalStartButton: Locator;
            modalEndInput: Locator;
            modalEndButton: Locator;
            expectedDefaultValue: DatePair;
            value: DatePair;
            valueDisplay: Locator;
        };
        receiverName: {
            label: Locator;
            input: Locator;
            expectedDefaultValue: string;
            tooltip: Locator;
            expectedTooltipText: string;
            value: string;
        };
        resultMessage: {
            label: Locator;
            input: Locator;
            expectedDefaultValue: string;
            tooltip: Locator;
            expectedTooltipText: string;
            value: string;
        };
        successType: {
            label: Locator;
            input: Locator;
            expectedInputText: string;
            expectedDefaultValue: string;
            tooltip: Locator;
            expectedTooltipText: string;
            value: string;
        };
    };

    readonly statusContainer: Locator;
    readonly statusRows: Locator;

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/admin/send-dash",
                title: "Receiver status dashboard - Admin",
                heading: testArgs.page.getByRole("heading", {
                    name: "Receiver Status Dashboard",
                }),
            },
            testArgs,
        );

        this.addMockRouteHandlers([this.createMockReceiverStatusHandler()]);
        this.addResponseHandlers([this.createMockReceiverStatusResponseHandler()]);

        const now = new Date();

        this._receiverStatus = [];
        this._timePeriodData = [];

        this.filterForm = this.page.getByRole("form", { name: "filter" });
        const dateRangeOverlay = this.page.getByRole("dialog").locator(".usa-modal-overlay");
        const dateRangeDefaultValue = [startOfDay(subDays(now, 2)), endOfDay(now)] as DatePair;
        this.filterFormInputs = {
            dateRange: {
                label: this.page.locator("label", {
                    hasText: "Date Range:",
                }),
                button: this.page.getByRole("button", {
                    name: "Change...",
                }),
                modalOverlay: dateRangeOverlay,
                expectedModalOverlayText: "Select date range to show. (Max 10 days span)FromToUpdate",
                modalOverlayCalendar: dateRangeOverlay.getByRole("application"),
                modalPrimaryButton: dateRangeOverlay.getByRole("button", {
                    name: "Update",
                }),
                modalStartInput: dateRangeOverlay.getByRole("textbox", {
                    name: "From",
                }),
                modalStartButton: dateRangeOverlay.getByRole("button").nth(0),
                modalEndInput: dateRangeOverlay.getByRole("textbox", {
                    name: "To",
                }),
                modalEndButton: dateRangeOverlay.getByRole("button").nth(1),
                expectedDefaultValue: dateRangeDefaultValue,
                value: dateRangeDefaultValue,
                valueDisplay: this.page.locator("span", { hasText: "🗓" }),
            },
            receiverName: {
                label: this.page.locator("label", {
                    hasText: "Date Range:",
                }),
                expectedTooltipText:
                    "Filter rows on just the first column's Organization name and Receiver setting name.",
                input: this.page.getByRole("textbox", {
                    name: "Receiver Name:",
                }),
                expectedDefaultValue: "",
                tooltip: this.page.getByTestId("tooltipWrapper").nth(0).getByRole("tooltip"),
                value: "",
            },
            resultMessage: {
                label: this.page.locator("label", {
                    hasText: "Results Message:",
                }),
                expectedTooltipText: "Filter rows on the Result Message details. This value is found in the details.",
                tooltip: this.page.getByTestId("tooltipWrapper").nth(1).getByRole("tooltip"),
                input: this.page.getByRole("textbox", {
                    name: "Results Message:",
                }),
                expectedDefaultValue: "",
                value: "",
            },
            successType: {
                label: this.page.locator("label", {
                    hasText: "Success Type:",
                }),
                expectedTooltipText: "Show only rows in one of these states.",
                tooltip: this.page.getByTestId("tooltipWrapper").nth(2).getByRole("tooltip"),
                input: this.page.getByRole("combobox", {
                    name: "Success Type:",
                }),
                expectedDefaultValue: "UNDEFINED",
                expectedInputText: "Show AllFailedMixed success",
                value: "UNDEFINED",
            },
        };

        this.statusContainer = this.page.locator(".rs-admindash-component");
        this.statusRows = this.statusContainer.locator("> *");
    }

    /**
     * Error expected additionally if user context isn't admin
     */
    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.testArgs.storageState === this.testArgs.adminLogin.path;
    }

    get receiverStatus() {
        return this._receiverStatus;
    }

    get timePeriodData() {
        return this._timePeriodData;
    }

    createMockReceiverStatusHandler(): RouteHandlerFulfillEntry {
        return [
            AdminReceiverStatusPage.API_RECEIVER_STATUS,
            (request) => {
                const url = new URL(request.url());
                const startDate = url.searchParams.get("start_date");
                const endDate = url.searchParams.get("end_date");
                const range = startDate && endDate ? ([new Date(startDate), new Date(endDate)] as DatePair) : undefined;

                return {
                    json: this.createMockReceiverStatuses(range),
                };
            },
        ];
    }

    createMockReceiverStatusResponseHandler(): ResponseHandlerEntry {
        return [
            AdminReceiverStatusPage.API_RECEIVER_STATUS,
            async (apiRes) => {
                const data: RSReceiverStatus[] = await apiRes.json();
                const url = new URL(apiRes.url());
                const startDate = url.searchParams.get("start_date");
                const endDate = url.searchParams.get("end_date");
                const range = startDate && endDate ? ([new Date(startDate), new Date(endDate)] as DatePair) : undefined;
                this._receiverStatus = data;
                this._timePeriodData = range ? this.createTimePeriodData({ data, range }) : [];
            },
        ];
    }

    createMockReceiverStatuses(...args: Parameters<typeof createMockGetReceiverStatus>) {
        return createMockGetReceiverStatus(...args);
    }

    createTimePeriodData(...args: Parameters<typeof createStatusTimePeriodData>) {
        return createStatusTimePeriodData(...args);
    }

    get receiverStatusRowsLocator() {
        const rows = this.statusContainer.locator("> .grid-row");

        return Object.assign(rows, {
            allCustom: async () => {
                return (await rows.all()).map((r) =>
                    Object.assign({}, r, {
                        title: this.getReceiverStatusRowTitle(r),
                        display: this.getReceiverStatusRowDisplay(r),
                        days: this.getReceiverStatusRowDisplayDays(r),
                    }),
                );
            },
            nthCustom: (nth: number) => {
                const row = rows.nth(nth);
                return Object.assign(row, {
                    title: this.getReceiverStatusRowTitle(row),
                    display: this.getReceiverStatusRowDisplay(row),
                    days: this.getReceiverStatusRowDisplayDays(row),
                });
            },
        });
    }

    getExpectedReceiverStatusRowTitle(organizationName: string, receiverName: string, successRate: number | string) {
        return [organizationName, receiverName, successRate, "%"].join("");
    }

    getExpectedDateRangeValueDisplay(startDate: Date, endDate: Date) {
        return `🗓 ${startDate.toLocaleDateString()} — ${endDate.toLocaleDateString()}`;
    }

    getReceiverStatusRowDisplayDayTimePeriods(day: Locator) {
        return day.locator(".slice");
    }

    getReceiverStatusRowDisplayDays(row: Locator) {
        const days = row.locator(".slices-row");
        return Object.assign(days, {
            allCustom: async () => {
                return (await days.all()).map((d) =>
                    Object.assign(d, {
                        timePeriods: this.getReceiverStatusRowDisplayDayTimePeriods(d),
                    }),
                );
            },
            nthCustom: (nth: number) => {
                const day = days.nth(nth);
                return Object.assign(day, {
                    timePeriods: this.getReceiverStatusRowDisplayDayTimePeriods(day),
                });
            },
        });
    }

    getReceiverStatusRowDisplay(row: Locator) {
        return row.locator("> .grid-row");
    }

    getReceiverStatusRowTitle(row: Locator) {
        return row.locator("> .title-column");
    }

    async updateFilters({
        dateRange,
        receiverName,
        resultMessage,
        successType,
    }: AdminReceiverStatusPageUpdateFiltersProps) {
        // API request will only fire if date ranges are different
        const isDateRangeDifferent =
            dateRange == null || this.getIsDateRangesDifferent(this.filterFormInputs.dateRange.value, dateRange.value);
        const isRequestAwaitedBool = dateRange != null && isDateRangeDifferent && dateRange.isRequestAwaited !== false;
        const p = isRequestAwaitedBool
            ? this.page.waitForRequest(AdminReceiverStatusPage.API_RECEIVER_STATUS)
            : Promise.resolve();

        if (dateRange && isDateRangeDifferent) {
            const { value, inputMethod } = dateRange;
            await this.updateFilterDateRange(...value, inputMethod);
        }
        if (receiverName != null && receiverName !== this.filterFormInputs.receiverName.value)
            await this.updateFilterReceiverName(receiverName);
        if (resultMessage != null && resultMessage !== this.filterFormInputs.resultMessage.value)
            await this.updateFilterResultMessage(resultMessage);
        if (successType != null && successType !== this.filterFormInputs.successType.value)
            await this.updateFilterSuccessType(successType);

        if (!isRequestAwaitedBool) return undefined as void;

        const req = await p;
        return req ? new URL(req.url()) : undefined;
    }

    async updateFilterDateRange(start: Date, end: Date, inputMethod: "textbox" | "calendar" = "textbox") {
        const {
            button,
            modalStartInput,
            modalEndInput,
            modalPrimaryButton,
            modalStartButton,
            modalEndButton,
            modalOverlayCalendar,
        } = this.filterFormInputs.dateRange;

        await button.click();

        if (inputMethod === "textbox") {
            await modalStartInput.fill(start.toLocaleDateString());
            await modalEndInput.fill(end.toLocaleDateString());
        } else {
            const targetFromDayLabel = format(start, "dd MMMM yyyy EEEE");
            const targetToDayLabel = format(end, "dd MMMM yyyy EEEE");
            const fromCalendarDay = modalOverlayCalendar.getByRole("button", {
                name: targetFromDayLabel,
            });
            const toCalendarDay = modalOverlayCalendar.getByRole("button", {
                name: targetToDayLabel,
            });

            await modalStartButton.click();
            await fromCalendarDay.click();

            await modalEndButton.click();
            await toCalendarDay.click();
        }

        await modalPrimaryButton.click();

        this.filterFormInputs.dateRange.value = [start, end];
    }

    async updateFilterReceiverName(name: string) {
        await this.filterFormInputs.receiverName.input.fill(name);
        this.filterFormInputs.receiverName.value = name;
    }

    async updateFilterResultMessage(msg: string) {
        await this.filterFormInputs.resultMessage.input.fill(msg);
        this.filterFormInputs.resultMessage.value = msg;
    }

    async updateFilterSuccessType(type: string) {
        await this.filterFormInputs.successType.input.selectOption(type);
        this.filterFormInputs.successType.value = type;
    }

    async resetFilters() {
        const resetValues = {
            dateRange: {
                value: this.filterFormInputs.dateRange.expectedDefaultValue,
            },
            receiverName: this.filterFormInputs.receiverName.expectedDefaultValue,
            resultMessage: this.filterFormInputs.resultMessage.expectedDefaultValue,
            successType: this.filterFormInputs.successType.expectedDefaultValue,
        };
        return await this.updateFilters(resetValues);
    }

    get expectedDateRangeLabelText() {
        const [start, end] = this.filterFormInputs.dateRange.value;
        return this.getExpectedDateRangeValueDisplay(start, end);
    }

    getExpectedStatusOrganizationUrl(rowI: number) {
        const { organizationName } = this.timePeriodData[rowI];

        return `/admin/orgsettings/org/${organizationName}`;
    }

    getExpectedStatusReceiverUrl(rowI: number) {
        const { organizationName, receiverName } = this.timePeriodData[rowI];

        return `/admin/orgreceiversettings/org/${organizationName}/receiver/${receiverName}/action/edit`;
    }

    getIsDateRangesDifferent(a: DatePair, b: DatePair) {
        return a[0] !== b[0] && a[1] !== b[1];
    }

    async testReceiverStatusDisplay(isSmoke = false) {
        const selectedSuccessRate = this.filterFormInputs.successType.value as SuccessRate;
        const [startDate, endDate] = this.filterFormInputs.dateRange.value;
        const statusRows = this.receiverStatusRowsLocator;
        const timePeriodData = this.timePeriodData.filter(
            (d) => selectedSuccessRate === SuccessRate.UNDEFINED || d.successRateType === selectedSuccessRate,
        );
        await expect(statusRows).toHaveCount(timePeriodData.length);

        const expectedDaysText = [
            dateShortFormat(startDate),
            dateShortFormat(subDays(endDate, 1)),
            dateShortFormat(endDate),
        ].join("            ");
        for (const [
            i,
            { days, successRate, organizationName, receiverName, successRateType },
        ] of timePeriodData.entries()) {
            const { title, display, days: daysLoc } = statusRows.nthCustom(i);

            const expectedTitleText = this.getExpectedReceiverStatusRowTitle(
                organizationName,
                receiverName,
                successRate,
            );
            const expectedClass = new RegExp(SUCCESS_RATE_CLASSNAME_MAP[successRateType]);

            await expect(title).toBeVisible();
            await expect(title).toHaveText(expectedTitleText);
            await expect(title).toHaveClass(expectedClass);

            await expect(display).toBeVisible();
            await expect(display).toHaveText(expectedDaysText);

            await expect(daysLoc).toHaveCount(days.length);

            for (const [i, { timePeriods }] of days.entries()) {
                const daySlices = daysLoc.nthCustom(i).timePeriods;
                await expect(daySlices).toHaveCount(timePeriods.length);

                for (const [i, { successRateType, entries }] of timePeriods.entries()) {
                    const sliceEle = daySlices.nth(i);
                    // filtering status isn't recalculated automatically, so do it here
                    const isResultFilterMatch =
                        this.filterFormInputs.resultMessage.value === ""
                            ? undefined
                            : entries.some((e) =>
                                  isConnectionResultMatch(
                                      e.connectionCheckResult,
                                      this.filterFormInputs.resultMessage.value,
                                  ),
                              );
                    const classStr = [
                        SUCCESS_RATE_CLASSNAME_MAP[successRateType],
                        MATCHING_FILTER_CLASSNAME_MAP[
                            isResultFilterMatch == null
                                ? MatchingFilter.NO_FILTER
                                : isResultFilterMatch
                                  ? MatchingFilter.FILTER_IS_MATCHED
                                  : MatchingFilter.FILTER_NOT_MATCHED
                        ],
                    ]
                        .filter(Boolean)
                        .join(" ");
                    const expectedClass = new RegExp(classStr);

                    await expect(sliceEle).toBeVisible();
                    await expect(sliceEle).toHaveClass(expectedClass);
                }
            }

            if (isSmoke && i === 0) {
                break;
            }
        }

        return true;
    }

    async testReceiverName() {
        const { organizationName, receiverName, successRate } = this.timePeriodData[1];

        const receiversStatusRows = this.receiverStatusRowsLocator;
        const expectedReceiverStatusRow = receiversStatusRows.nthCustom(0);
        const expectedReceiverStatusRowTitle = this.getExpectedReceiverStatusRowTitle(
            organizationName,
            receiverName,
            successRate,
        );

        await expect(receiversStatusRows).toHaveCount(this.timePeriodData.length);

        await this.updateFilters({
            receiverName,
        });

        await expect(receiversStatusRows).toHaveCount(1);
        await expect(expectedReceiverStatusRow).toBeVisible();
        await expect(expectedReceiverStatusRow.title).toHaveText(expectedReceiverStatusRowTitle);

        await this.resetFilters();

        await expect(receiversStatusRows).toHaveCount(this.timePeriodData.length);

        return true;
    }

    async testReceiverMessage() {
        // get first entry's result from all-fail receiver's first day -> third time period
        const receiverI = 0;
        const dayI = 0;
        const timePeriodI = 2;
        const entryI = 0;
        const { days } = this.timePeriodData[receiverI];
        const { connectionCheckResult } = days[dayI].timePeriods[timePeriodI].entries[entryI];

        const receiversStatusRows = this.receiverStatusRowsLocator;

        await this.updateFilters({
            resultMessage: connectionCheckResult,
        });

        for (const [i, { days }] of this.timePeriodData.entries()) {
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

        await this.resetFilters();

        await this.testReceiverStatusDisplay();

        return true;
    }

    async testReceiverOrgLinks(isSmoke = false) {
        const rows = this.receiverStatusRowsLocator;

        for (const [i, { organizationName }] of this.timePeriodData.entries()) {
            const row = rows.nthCustom(i);

            const link = row.title.getByRole("link", { name: organizationName, exact: true }).first();
            const expectedUrl = this.getExpectedStatusOrganizationUrl(i);
            await expect(link).toBeVisible();
            const p = this.page.route(`/api/settings/organizations/${organizationName}`, (route) =>
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
            await expect(this.page).toHaveURL(expectedUrl);
            await p;
            await this.page.goBack();

            if (isSmoke && i === 0) {
                break;
            }
        }

        return true;
    }

    async testReceiverTimePeriodModals(isSmoke = false) {
        const overlay = this.filterFormInputs.dateRange.modalOverlay;

        for (const [i, { days }] of this.timePeriodData.entries()) {
            const { days: daysLoc } = this.receiverStatusRowsLocator.nthCustom(i);

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

            if (isSmoke && i === 0) {
                break;
            }
        }

        return true;
    }

    async testReceiverLinks(isSmoke = false) {
        const rows = this.receiverStatusRowsLocator;

        for (const [i, { receiverName }] of this.timePeriodData.entries()) {
            const row = rows.nthCustom(i);

            const link = row.title.getByRole("link", {
                name: receiverName,
            });
            await expect(link).toBeVisible();
            await link.click();
            await expect(this.page).toHaveURL(this.getExpectedStatusReceiverUrl(i));
            await this.page.goBack();

            if (isSmoke && i === 0) {
                break;
            }
        }

        return true;
    }
}
