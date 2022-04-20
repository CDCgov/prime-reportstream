import AuthResource from "./AuthResource";

export default class OrganizationResource extends AuthResource {
    readonly name: string = "name";
    readonly description: string = "description";
    readonly jurisdiction: string = "jurisdiction";
    readonly stateCode: string = "state";
    readonly countyName: string = "county";

    pk() {
        return this.name;
    }

    static urlRoot = `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations`;

    static url(params: { orgname: string }): string {
        return `${this.urlRoot}/${params.orgname}`;
    }
}
