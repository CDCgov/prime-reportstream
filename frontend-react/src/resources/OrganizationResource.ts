import AuthResource from './AuthResource';

export default class OrganizationResource extends AuthResource {
    readonly name: string = "name";
    readonly description: string = "description";
    readonly jurisdiction: string = "jurisdiction";
    readonly stateCode: string = "state"
    readonly countyName: string = "county"

    pk(){
        return this.name;
    }

    static urlRoot = 'http://localhost:7071/api/settings/organizations';
}