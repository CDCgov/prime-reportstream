import { formatDate } from "../utils/misc";
import config from "../config";

import AuthResource from "./AuthResource";

const { RS_API_URL } = config;

export class AdmSendFailuresResource extends AuthResource {
    /* the unique id for the action */
    readonly actionId: number = 0;
    /* the uuid for this report */
    readonly reportId: string = "";
    /* Org destination name of the receiver that failed */
    readonly receiver: string = "";
    /* Filename for the data that's prepared for forwarding but failing */
    readonly fileName: string = "";
    /* the time that the particular error happened */
    readonly failedAt: string = "";
    /* The original action that failed had a url. These are the cgi params. */
    readonly actionParams: string = "";
    /* The long error message generated when the upload failed. */
    readonly actionResult: string = "";
    /* The body portion of the original action url. Contains the location of the file that failed to forward */
    readonly bodyUrl: string = "";
    /* The parsed receiver. It should be the same as receiver field above */
    readonly reportFileReceiver: string = "";

    pk() {
        return `actionid-${this.actionId}`;
    }

    static urlRoot = `${RS_API_URL}/api/adm/getsendfailures`;

    static url(params: { daysToShow: number }): string {
        return `${this.urlRoot}?days_to_show=${params.daysToShow}`;
    }

    /**
     * Used by filter edit box ui to show only matched elements.
     * Allows some data to be excluded or cleaned up
     * @param search {string}
     */
    filterMatch(search: string | null): boolean {
        if (!search) {
            // no search returns EVERYTHING
            return true;
        }
        // combine all elements to be searched.
        // Date is search as it is displayed AND original timestamp
        const datestr = formatDate(this.failedAt);
        const fullstr =
            `${this.reportId} ${this.receiver} ${this.fileName} ${this.actionResult} ${this.bodyUrl} ${datestr} ${this.failedAt}`.toLowerCase();
        return fullstr.includes(`${search.toLowerCase()}`);
    }
}
