import config from "../config";

import AuthResource from "./AuthResource";

const { RS_API_URL } = config;

export default class FacilityResource extends AuthResource {
    readonly facility: string | undefined = "";
    readonly location: string | undefined = "";
    readonly CLIA: string | undefined = "";
    readonly positive: string | undefined = "";
    readonly total: string | undefined = "";

    pk() {
        return `${this.facility}-${this.location}`;
    }

    /* INFO
       since we won't be using urlRoot to build our urls we still need to tell rest hooks
       how to uniquely identify this Resource

       >>> Kevin Haube, October 4, 2021
    */
    static get key() {
        return "FacilityResource";
    }

    /* INFO
       This method is invoked by calling FacilityResource.list() in a useResource() hook. This
       replaces the need for a urlRoot variable.

       <<< Kevin Haube , October 4, 2021
    */
    static listUrl(searchParams: { reportId: string }): string {
        if (searchParams && Object.keys(searchParams).length) {
            const { reportId } = searchParams;
            return `${RS_API_URL}/api/history/report/${reportId}/facilities`;
        }
        throw new Error("Facilities require a reportId to retrieve");
    }
}
