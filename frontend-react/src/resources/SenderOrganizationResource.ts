import SenderAuthResource from "./SenderAuthResource";

export default class SenderOrganizationResource extends SenderAuthResource {
    readonly name: string = "name";
    readonly description: string = "description";
    readonly jurisdiction: string = "jurisdiction";
    readonly stateCode: string = "state"
    readonly countyName: string = "county"

    pk(){
        return this.name;
    }

    static urlRoot = `${SenderAuthResource.getBaseUrl()}/api/settings/organizations`;
}