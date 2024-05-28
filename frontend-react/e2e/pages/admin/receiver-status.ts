import { Locator, Request } from "@playwright/test";
import { endOfDay, format, startOfDay, subDays } from "date-fns";
import { createStatusTimePeriodData } from "../../../src/components/Admin/AdminReceiverDashboard.utils";
import { AdmConnStatusDataType } from "../../../src/resources/AdmConnStatusResource";
import { DatePair } from "../../../src/utils/DateTimeUtils";
import { createMockGetReceiverStatus } from "../../mocks/receiverStatus";
import { TestArgs } from "../../test";
import { BasePage, GotoOptions, GotoRouteHandlers, RouteHandlers } from "../BasePage";
export class AdminReceiverStatusPage extends BasePage {
    static readonly API_RECEIVER_STATUS = "/api/adm/listreceiversconnstatus?*";
    readonly receiverStatus?: AdmConnStatusDataType[];
    readonly timePeriodData?: ReturnType<typeof createStatusTimePeriodData>;

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
            value: DatePair;
        };
        receiverName: {
            label: Locator;
            input: Locator;
            expectedInputDefaultValue: string;
            tooltip: Locator;
            expectedTooltipText: string;
            value: string;
        };
        resultMessage: {
            label: Locator;
            input: Locator;
            expectedInputDefaultValue: string;
            tooltip: Locator;
            expectedTooltipText: string;
            value: string;
        };
        successType: {
            label: Locator;
            input: Locator;
            expectedInputText: string;
            expectedInputDefaultValue: string;
            tooltip: Locator;
            expectedTooltipText: string;
            value: string;
        };
    };

    readonly statusContainer: Locator;
    readonly statusRows: Locator;

    constructor(testArgs: TestArgs) {
        super(
            {
                url: "/admin/send-dash",
                title: "Receiver status dashboard",
                heading: testArgs.page.getByRole("heading", {
                    name: "Receiver Status Dashboard",
                }),
            },
            testArgs,
        );

        const now = new Date();

        this.filterForm = this.page.getByRole("form", { name: "filter" });
        const dateRangeOverlay = this.page
            .getByRole("dialog")
            .locator(".usa-modal-overlay");
        this.filterFormInputs = {
            dateRange: {
                label: this.page.locator("label", {
                    hasText: "Date Range:",
                }),
                button: this.page.getByRole("button", {
                    name: "Change...",
                }),
                modalOverlay: dateRangeOverlay,
                expectedModalOverlayText:
                    "Select date range to show. (Max 10 days span)FromToUpdate",
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
                value: [startOfDay(subDays(now, 2)), endOfDay(now)],
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
                expectedInputDefaultValue: "",
                tooltip: this.page
                    .getByTestId("tooltipWrapper")
                    .nth(0)
                    .getByRole("tooltip"),
                value: "",
            },
            resultMessage: {
                label: this.page.locator("label", {
                    hasText: "Result Message:",
                }),
                expectedTooltipText:
                    "Filter rows on the Result Message details. This value is found in the details.",
                tooltip: this.page
                    .getByTestId("tooltipWrapper")
                    .nth(1)
                    .getByRole("tooltip"),
                input: this.page.getByRole("textbox", {
                    name: "Result Message:",
                }),
                expectedInputDefaultValue: "",
                value: "",
            },
            successType: {
                label: this.page.locator("label", {
                    hasText: "Success Type:",
                }),
                expectedTooltipText: "Show only rows in one of these states.",
                tooltip: this.page
                    .getByTestId("tooltipWrapper")
                    .nth(2)
                    .getByRole("tooltip"),
                input: this.page.getByRole("listbox", {
                    name: "Success Type:",
                }),
                expectedInputDefaultValue: "UNDEFINED",
                expectedInputText: "Show AllFailedMixed success",
                value: "UNDEFINED",
            },
        };

        this.statusContainer = this.page.locator(".rs-admindash-component");
        this.statusRows = this.statusContainer.locator("> *");
    }

    goto(opts?: GotoOptions, { mock, handlers }: GotoRouteHandlers = {}) {
        return super.goto(opts, {
            mock: mock ?? this.createMockReceiverStatusHandler(),
            handlers,
        });
    }

    createMockReceiverStatusHandler(
        ...args: Parameters<typeof createMockGetReceiverStatus>
    ): RouteHandlers {
        return {
            [AdminReceiverStatusPage.API_RECEIVER_STATUS]: (route) => {
                return route.fulfill({
                    json: this.createMockReceiverStatuses(...args),
                });
            },
        };
    }

    createMockReceiverStatuses(
        ...args: Parameters<typeof createMockGetReceiverStatus>
    ) {
        return createMockGetReceiverStatus(...args);
    }

    createTimePeriodData(
        ...args: Parameters<typeof createStatusTimePeriodData>
    ) {
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

    getExpectedReceiverStatusRowTitle(
        organizationName: string,
        receiverName: string,
        successRate: number | string,
    ) {
        return [organizationName, receiverName, successRate, "%"].join("");
    }

    getExpectedDateRangeLabel(startDate: Date, endDate: Date) {
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
                        timePeriods:
                            this.getReceiverStatusRowDisplayDayTimePeriods(d),
                    }),
                );
            },
            nthCustom: (nth: number) => {
                const day = days.nth(nth);
                return Object.assign(day, {
                    timePeriods:
                        this.getReceiverStatusRowDisplayDayTimePeriods(day),
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

    async updateFilterDateRange(
        start: Date,
        end: Date,
        isRequestAwaited?: true,
        inputMethod?: "textbox" | "calendar",
    ): Promise<URL>;
    async updateFilterDateRange(
        start: Date,
        end: Date,
        isRequestAwaited: false,
        inputMethod?: "textbox" | "calendar",
    ): Promise<void>;
    async updateFilterDateRange(
        start: Date,
        end: Date,
        isRequestAwaited = true,
        inputMethod: "textbox" | "calendar" = "textbox"
    ) {
        const isRequestAwaitedBool = Boolean(isRequestAwaited);
        const { button, modalStartInput, modalEndInput, modalPrimaryButton, modalStartButton, modalEndButton, modalOverlayCalendar } =
            this.filterFormInputs.dateRange;
        const p = isRequestAwaitedBool
            ? this.page.waitForRequest(
                  AdminReceiverStatusPage.API_RECEIVER_STATUS,
              )
            : Promise.resolve();

        await button.click();

        if(inputMethod === "textbox") {
            await modalStartInput.fill(start.toLocaleDateString());
            await modalEndInput.fill(end.toLocaleDateString());
        } else {
            const targetFromDayLabel = format(
                start,
                "dd MMMM yyyy EEEE",
            );
            const targetToDayLabel = format(
                end,
                "dd MMMM yyyy EEEE",
            );
            const fromCalendarDay =
                modalOverlayCalendar.getByRole("button", {
                    name: targetFromDayLabel,
                });
            const toCalendarDay =
                modalOverlayCalendar.getByRole("button", {
                    name: targetToDayLabel,
                });

            await modalStartButton.click();
            await fromCalendarDay.click();

            await modalEndButton.click();
            await toCalendarDay.click();
        }

        await modalPrimaryButton.click();

        if (!isRequestAwaitedBool) return undefined as void;

        const reqUrl = new URL((await (p as Promise<Request>)).url());
        const reqStart = reqUrl.searchParams.get("start_date");
        const reqEnd = reqUrl.searchParams.get("end_date");
        expect(reqStart).toBeDefined();
        expect(reqEnd);
        this.filterFormInputs.dateRange.value = [
            new Date(reqStart!),
            new Date(reqEnd!),
        ];

        return reqUrl;
    }

    get expectedDateRangeLabelText() {
        const [start, end] = this.filterFormInputs.dateRange.value;
        return this.getExpectedDateRangeLabel(start, end);
    }
}
