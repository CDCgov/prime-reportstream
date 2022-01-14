import OrgSettingsBaseResource from "./OrgSettingsBaseResource";

export default class OrgSenderSettingsResource extends OrgSettingsBaseResource {
    organizationName: string = "";
    format: string = "";
    topic: string = "";
    customerStatus: string = "";
    schemaName: string = "";
    keys: string[] = [];
    processingType: string = "";

    pk() {
        return this.name;
    }

    static get key() {
        return "OrgSenderSettingsResource";
    }

    static listUrl(params: { orgname: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/senders`;
    }

    static url(params: { orgname: string; sendername: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/senders/${params.sendername}`;
    }

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
            })
        );
    }
}
