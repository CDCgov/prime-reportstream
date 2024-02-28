import OrgSettingsBaseResource from "./OrgSettingsBaseResource";
import config from "../config";

const { RS_API_URL } = config;

export default class OrgSettingsResource extends OrgSettingsBaseResource {
    // pulls in other fields from OrgSettingsBaseResource
    description = "";
    jurisdiction = "";
    stateCode: string | null = "";
    countyName: string | null = "";
    filters: object = [];

    static readonly key = "OrgSettingsResource";

    static listUrl(
        _params?: Readonly<Record<string, string | number | boolean>>,
    ): string {
        return `${RS_API_URL}/api/settings/organizations`;
    }

    static url(params: { orgname: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgname}`;
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
        const fullstr =
            `${this.name} ${this.description} ${this.jurisdiction} ${this.stateCode} ${this.countyName}`.toLowerCase();
        return fullstr.includes(`${search.toLowerCase()}`);
    }
}
