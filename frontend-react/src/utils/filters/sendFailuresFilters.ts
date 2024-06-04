import { RSSendFailure } from "../../hooks/api/UseSendFailures/UseSendFailures";
import { formatDate } from "../misc";

/**
 * Used by filter edit box ui to show only matched elements.
 * Allows some data to be excluded or cleaned up
 * @param search {string}
 */
export function filterMatch(
    obj: RSSendFailure,
    search: string | null,
): boolean {
    if (!search) {
        // no search returns EVERYTHING
        return true;
    }
    // combine all elements to be searched.
    // Date is search as it is displayed AND original timestamp
    const datestr = formatDate(obj.failedAt);
    const fullstr =
        `${obj.reportId} ${obj.receiver} ${obj.fileName} ${obj.actionResult} ${obj.bodyUrl} ${datestr} ${obj.failedAt}`.toLowerCase();
    return fullstr.includes(`${search.toLowerCase()}`);
}
