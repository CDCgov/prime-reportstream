import OrgSettingsBaseResource from "./OrgSettingsBaseResource";

export interface FilterData {
    topic: string;
    jurisdictionalFilter: string[] | null; // | undefined?
    qualityFilter: string[] | null;
    routingFilter: string[] | null;
    processingModeFilter: string[] | null;
}

// fyi the API defines these as CamelCased but the DB seems to have them UPPERCASED
// so, we can't really have an enumerator.
// enum JurisdictionData {
//     FEDERAL = 'FEDERAL',
//     STATE = 'STATE',
//     COUNTY = 'COUNTY'
// }

export default class OrgSettingsResource extends OrgSettingsBaseResource {
    // pulls in other fields from OrgSettingsBaseResource
    readonly description: string = "";
    readonly jurisdiction: string = "";
    readonly stateCode: string = "";
    readonly countyName: string = "";
    readonly filter: FilterData[] = [];

    static get key() {
        return "OrgSettingsResource";
    }

    static listUrl(params: {}): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations`;
    }

    static url(params: { orgname: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}`;
    }

    filterMatch(search: string | null): boolean {
        if (!search) {
            // no search returns EVERYTHING
            return true;
        }
        // experimenting with matching.
        // combine all the search terms, split into words, prefix match
        // COULD include deeper meta data to search? we'd have to flatten the array of objects
        const fullstr =
            `${this.name} ${this.description} ${this.jurisdiction} ${this.stateCode} ${this.stateCode}`.toLowerCase();
        return fullstr.includes(`${search.toLowerCase()}`);
    }
}
