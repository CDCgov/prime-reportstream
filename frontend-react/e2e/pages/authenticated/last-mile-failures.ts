import { MOCK_GET_RESEND, MOCK_GET_SEND_FAILURES } from "../../mocks/lastMilefailures";
import { BasePage, BasePageTestArgs, RouteHandlerFulfillEntry } from "../BasePage";

export class LastMileFailuresPage extends BasePage {
    static readonly URL_LAST_MILE = "/admin/lastmile";
    static readonly API_GET_SEND_FAILURES = "/api/adm/getsendfailures?days_to_show=15";
    static readonly API_GET_RESEND = "/api/adm/getresend?days_to_show=15";

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
}
