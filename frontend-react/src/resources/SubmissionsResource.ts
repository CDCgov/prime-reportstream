import config from "../config";

import AuthResource from "./AuthResource";

const { RS_API_URL } = config;

type SubmissionsResourceParams = {
    organization: string;
    sortdir: string;
    sortcol: string;
    cursor: string;
    since: string;
    until: string;
    pageSize: number;
    showFailed: boolean;
};

const FALLBACKDATE = "2020-01-01T00:00:00.000Z";

export default class SubmissionsResource extends AuthResource {
    readonly submissionId: number = 0;
    readonly timestamp: string = FALLBACKDATE; // format is "2022-02-01T15:11:58.200754Z"
    readonly sender: string = "";
    readonly httpStatus: number = 0;
    readonly externalName: string = "";
    readonly id: string | undefined;
    readonly topic: string = "";
    readonly reportItemCount: number = 0;
    readonly warningCount: number = 0;
    readonly errorCount: number = 0;

    pk() {
        // For failed submissions, the report id will be null. Rest Hooks will not cache a record without a pk, thus
        // falling back to using timestamp
        return `${this.timestamp} ${this.id}`;
    }

    static get key() {
        return "SubmissionsResource";
    }

    static listUrl(searchParams: SubmissionsResourceParams): string {
        const url = new URL(`
            ${RS_API_URL}/api/waters/org/${searchParams.organization}/submissions`);
        url.searchParams.append("pageSize", searchParams.pageSize.toString());
        url.searchParams.append("cursor", searchParams.cursor);
        url.searchParams.append("since", searchParams.since);
        url.searchParams.append("until", searchParams.until);
        url.searchParams.append("sortcol", searchParams.sortcol);
        url.searchParams.append("sortdir", searchParams.sortdir);
        url.searchParams.append("showfailed", String(searchParams.showFailed));
        return url.href;
    }

    isSuccessSubmitted(): boolean {
        return this.id !== null;
    }

    /**
     * compareFunction for sorting. Sonar wants it in a function for complexity reasons.
     * @param a {SubmissionsResource}
     * @param b {SubmissionsResource}
     * @return {number}  typical compareFunction result -1, 0, 1
     */
    static sortByCreatedAt(
        a: SubmissionsResource,
        b: SubmissionsResource,
    ): number {
        // format "2022-02-01T15:11:58.200754Z" means we can compare strings without converting to dates
        // since it's in descending time format (aka year, month, day, hour, min, sec)
        if (a.timestamp === b.timestamp) {
            return 0;
        }
        return a.timestamp > b.timestamp ? -1 : 1;
    }
}
