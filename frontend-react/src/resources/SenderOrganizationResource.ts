import config from "../config";

import SenderAuthResource from "./SenderAuthResource";

const { RS_API_URL } = config;

export default class SenderOrganizationResource extends SenderAuthResource {
    readonly name: string = "name";
    readonly description: string = "description";
    readonly jurisdiction: string = "jurisdiction";
    readonly stateCode: string = "state";
    readonly countyName: string = "county";

    pk() {
        return this.name;
    }

    static urlRoot = `${RS_API_URL}/api/settings/organizations`;
}
