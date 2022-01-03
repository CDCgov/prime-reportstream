import AuthResource from "./AuthResource";

export interface MetaData {
    version: number;
    createdBy: string;
    createdAt: Date;
}

/**
 * This is an ABSTRACT base class for all the fields that are common to all the various "settings" api calls.
 */
export default abstract class OrgSettingsBaseResource extends AuthResource {
    readonly name: string = "";
    readonly meta: MetaData[] = [];

    pk() {
        return this.name;
    }
}
