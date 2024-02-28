import OrgSettingsBaseResource from "./OrgSettingsBaseResource";
import config from "../config";

const { RS_API_URL } = config;

export default class OrgReceiverSettingsResource extends OrgSettingsBaseResource {
    organizationName = "";
    topic = "";
    customerStatus = "";
    translation: object = {};
    description = "";
    jurisdictionalFilter: object = [];
    qualityFilter: object = [];
    routingFilter: object = [];
    processingModeFilter: object = [];
    reverseTheQualityFilter = false;
    deidentify = false;
    timing: object = [];
    transport: object = [];
    externalName = "";
    timeZone = "";
    dateTimeFormat = "";

    pk() {
        return this.name;
    }

    static readonly key = "OrgReceiverSettingsResource";

    static listUrl(params: { orgname: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgname}/receivers`;
    }

    static url(params: { orgname: string; receivername: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgname}/receivers/${params.receivername}`;
    }
}
