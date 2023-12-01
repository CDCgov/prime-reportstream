import config from "../config";

import OrgSettingsBaseResource from "./OrgSettingsBaseResource";

const { RS_API_URL } = config;

export default class OrgReceiverSettingsResource extends OrgSettingsBaseResource {
    organizationName: string = "";
    topic: string = "";
    customerStatus: string = "";
    translation: object = {};
    description: string = "";
    jurisdictionalFilter: object = [];
    qualityFilter: object = [];
    routingFilter: object = [];
    processingModeFilter: object = [];
    reverseTheQualityFilter: boolean = false;
    deidentify: boolean = false;
    timing: object = [];
    transport: object = [];
    externalName: string = "";
    timeZone: string = "";
    dateTimeFormat: string = "";

    pk() {
        return this.name;
    }

    static get key() {
        return "OrgReceiverSettingsResource";
    }

    static listUrl(params: { orgId: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgId}/receivers`;
    }

    static url(params: { orgId: string; entityId: string }): string {
        return `${RS_API_URL}/api/settings/organizations/${params.orgId}/receivers/${params.entityId}`;
    }
}
