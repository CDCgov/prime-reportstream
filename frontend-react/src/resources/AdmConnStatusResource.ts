import config from "../config";

import AuthResource from "./AuthResource";

const { RS_API_URL } = config;

/** having the type separate makes unit tests easier **/
export type AdmConnStatusDataType = {
    /* the unique id  */
    readonly receiverConnectionCheckResultId: number;
    readonly organizationId: number;
    readonly receiverId: number;
    readonly connectionCheckResult: string;
    readonly connectionCheckSuccessful: boolean;
    readonly connectionCheckStartedAt: string;
    readonly connectionCheckCompletedAt: string;
    readonly organizationName: string;
    readonly receiverName: string;
};

export class AdmConnStatusResource
    extends AuthResource
    implements AdmConnStatusDataType
{
    // would be nice if we didn't have to inline repeat the type above,
    // but I can't figure out an alternative using templates.
    // We COULD put it into a `data: AdmConnStatusDataType` variable, but
    // the Resource base class wants all the variables as part of the class.
    readonly receiverConnectionCheckResultId: number = 0;
    readonly organizationId: number = 0;
    readonly receiverId: number = 0;
    readonly connectionCheckResult: string = "";
    readonly connectionCheckSuccessful: boolean = false;
    readonly connectionCheckStartedAt: string = "";
    readonly connectionCheckCompletedAt: string = "";
    readonly organizationName: string = "";
    readonly receiverName: string = "";

    pk() {
        return `${AdmConnStatusResource.key}-${this.receiverConnectionCheckResultId}`;
    }

    static get key() {
        return "connstatusresource";
    }

    static listUrl(params: {
        startDate: string; // Date().toISOString
        endDate?: string | undefined; // Date().toISOString
    }): string {
        const base = `${RS_API_URL}/api/adm/listreceiversconnstatus?start_date=${params.startDate}`;
        if (params?.endDate) {
            return `${base}&end_date=${params.endDate}`;
        }
        return base;
    }

    /**
     * Used by filter edit box ui to show only matched elements.
     * Allows some data to be excluded or cleaned up
     * @param search {string}
     */
    filterOnName(search: string | null): boolean {
        if (!search) {
            return true; // no search returns EVERYTHING
        }
        // combine all elements to be searched.
        return `${this.organizationName} ${this.receiverName}`
            .toLowerCase()
            .includes(`${search.toLowerCase()}`);
    }

    filterOnCheckResultStr(search: string | null): boolean {
        if (!search) {
            return true; // no search returns EVERYTHING
        }
        return `${this.connectionCheckResult}`
            .toLowerCase()
            .includes(`${search.toLowerCase()}`);
    }
}
