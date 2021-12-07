import SenderAuthResource from "./SenderAuthResource";

export default class SenderOrganizationResource extends SenderAuthResource {
    readonly name: string = "name";
    readonly description: string = "description";
    readonly jurisdiction: string = "jurisdiction";
    readonly stateCode: string = "state";
    readonly countyName: string = "county";

    pk() {
        return this.name;
    }

    static urlRoot = `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations`;
}
