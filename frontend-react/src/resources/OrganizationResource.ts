import config from "../config";

import AuthResource from "./AuthResource";

const { RS_API_URL } = config;

export default class OrganizationResource extends AuthResource {
    readonly name: string = "name";
    readonly description: string = "description";
    readonly jurisdiction: string = "jurisdiction";
    readonly stateCode: string = "state";
    readonly countyName: string = "county";

    pk() {
        return this.name;
    }

    static urlRoot = `${RS_API_URL}/api/settings/organizations`;

    static url(params: { orgname: string }): string {
        return `${this.urlRoot}/${params.orgname}`;
    }
}
