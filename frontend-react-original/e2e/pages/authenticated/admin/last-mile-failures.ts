import { expect, Locator } from "@playwright/test";
import { startOfDay, subDays } from "date-fns";
import { tableRows } from "../../../helpers/utils";
import { MOCK_GET_RESEND, MOCK_GET_SEND_FAILURES } from "../../../mocks/lastMilefailures";
import { BasePage, BasePageTestArgs, RouteHandlerFulfillEntry } from "../../BasePage";

export class LastMileFailuresPage extends BasePage {
    static readonly URL_LAST_MILE = "/admin/lastmile";
    static readonly API_GET_SEND_FAILURES = "/api/adm/getsendfailures?days_to_show=15";
    static readonly API_GET_RESEND = "/api/adm/getresend?days_to_show=15";

    readonly filterFormInputs: {
        filter: {
            input: Locator;
        },
        daysToShow: {
            input: Locator;
            button: Locator
        }
    };

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: LastMileFailuresPage.URL_LAST_MILE,
                title: "Last Mile Failures",
                heading: testArgs.page.getByRole("heading", {
                    name: "Last Mile Failures",
                }),
            },
            testArgs,
        );

        this.addMockRouteHandlers([this.createMockGetSendFailuresHandler(), this.createMockGetResendHandler()]);
        this.filterFormInputs = {
            filter: {
                input: this.page.locator("#input_filter")
            },
            daysToShow: {
                input: this.page.locator("#days_to_show"),
                button: this.page.getByRole("button", {
                    name: "Refresh",
                }),
            }
        }
    }


    createMockGetSendFailuresHandler(): RouteHandlerFulfillEntry {
        return [
            LastMileFailuresPage.API_GET_SEND_FAILURES,
            () => {
                return {
                    json: MOCK_GET_SEND_FAILURES,
                };
            },
        ];
    }

    createMockGetResendHandler(): RouteHandlerFulfillEntry {
        return [
            LastMileFailuresPage.API_GET_RESEND,
            () => {
                return {
                    json: MOCK_GET_RESEND,
                };
            },
        ];
    }

    async tableColumnDateTimeInRange(
        daysToShow: number,
    ) {
        let datesInRange = true;
        const rowCount = await tableRows(this.page).count();
        const now = new Date();
        const targetFrom = startOfDay(subDays(now, daysToShow));

        for (let i = 0; i < rowCount; i++) {
            const columnValue = await tableRows(this.page).nth(i).locator("td").nth(0).innerText();
            const columnDate = new Date(columnValue);

            if (!(columnDate >= targetFrom)) {
                datesInRange = false;
                break;
            }
        }
        return datesInRange;
    }

    async testReportId(
        reportId: string,
    ) {
        const rowCount = await tableRows(this.page).count();

        for (let i = 0; i < rowCount; i++) {
            const columnValue = await tableRows(this.page).nth(i).locator("td").nth(1).innerText();

            expect(reportId).toEqual(columnValue);
        }

        return true;
    }
}
