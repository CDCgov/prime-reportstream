import OrgSettingsBaseResource from "./OrgSettingsBaseResource";
import config from "../config";

const { RS_API_URL } = config;

export default class OrgSenderSettingsResource extends OrgSettingsBaseResource {
    organizationName = "";
    format = "";
    topic = "";
    customerStatus = "";
    schemaName = "";
    keys: object = [];
    processingType = "";
    allowDuplicates = false;

    pk() {
        return this.name;
    }

    static readonly key = "OrgSenderSettingsResource";

    static listUrl(params: { orgname: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgname}/senders`;
    }

    static url(params: { orgname: string; sendername: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgname}/senders/${params.sendername}`;
    }
}
