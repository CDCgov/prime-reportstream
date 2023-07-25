import config from "../config";

import OrgSettingsBaseResource from "./OrgSettingsBaseResource";

const { RS_API_URL } = config;

export default class OrgSenderSettingsResource extends OrgSettingsBaseResource {
    organizationName: string = "";
    format: string = "";
    topic: string = "";
    customerStatus: string = "";
    schemaName: string = "";
    keys: object = [];
    processingType: string = "";
    allowDuplicates: boolean = false;

    pk() {
        return this.name;
    }

    static get key() {
        return "OrgSenderSettingsResource";
    }

    static listUrl(params: { orgname: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgname}/senders`;
    }

    static url(params: { orgname: string; sendername: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgname}/senders/${params.sendername}`;
    }
}
