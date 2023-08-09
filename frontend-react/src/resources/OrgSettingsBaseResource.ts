import AuthResource from "./AuthResource";

export interface MetaTaggedResource {
    version: number;
    createdBy: string;
    createdAt: string;
}

/**
 * This is an ABSTRACT base class for all the fields that are common to all the various "settings" api calls.
 */
export default abstract class OrgSettingsBaseResource extends AuthResource {
    name: string = "";
    readonly version: number = 0;
    readonly createdBy: string = "";
    readonly createdAt: string = "";

    pk() {
        return this.name;
    }

    // we had to build our own override for delete() because the api was not returning
    // the full response expected by react. Once that api change is made, there should
    // be no need for this method and delete() should be used.
    static deleteSetting() {
        const endpoint = this.endpointMutate();
        return this.memo("#delete", () =>
            endpoint.extend({
                fetch(params) {
                    // @ts-ignore
                    return endpoint.fetch.call(this, params).then(() => params);
                },

                method: "DELETE",
                schema: null,
            }),
        );
    }
}
