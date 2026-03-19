import { API_WATERS_REPORT } from "./report-details";
import { URL_SUBMISSION_HISTORY } from "./submission-history";
import { MOCK_GET_SUBMISSION_HISTORY } from "../../mocks/submissionHistory";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../BasePage";

export const id = "73e3cbc8-9920-4ab7-871f-843a1db4c074";
export class SubmissionsDetailsPage extends BasePage {
    static readonly URL_SUBMISSIONS_DETAILS = `${URL_SUBMISSION_HISTORY}/${id}`;

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: SubmissionsDetailsPage.URL_SUBMISSIONS_DETAILS,
                title: "ReportStream - CDC's free, interoperable data transfer platform",
            },
            testArgs,
        );

        this.addMockRouteHandlers([this.createMockSubmissionHistoryHandler(MOCK_GET_SUBMISSION_HISTORY)]);
    }

    createMockSubmissionHistoryHandler(mockFileName: any, responseStatus = 200): RouteHandlerFulfillEntry {
        return [
            `${API_WATERS_REPORT}/${id}/history`,
            () => {
                return {
                    json: mockFileName,
                    status: responseStatus,
                };
            },
        ];
    }

    /**
     * Error expected additionally if user context isn't admin
     */
    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.testArgs.storageState === this.testArgs.adminLogin.path;
    }
}
