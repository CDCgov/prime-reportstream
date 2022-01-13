import OrgSettingsBaseResource from "./OrgSettingsBaseResource";

export default class OrgReceiverSettingsResource extends OrgSettingsBaseResource {
    organizationName: string = "";
    topic: string = "";
    customerStatus: string = "";
    schemaName: string = "";
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

    pk() {
        return this.name;
    }

    static get key() {
        return "OrgReceiverSettingsResource";
    }

    static listUrl(params: { orgname: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/receivers`;
    }

    static url(params: { orgname: string; receivername: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/receivers/${params.receivername}`;
    }
}
