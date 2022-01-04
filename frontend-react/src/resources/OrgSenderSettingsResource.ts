import OrgSettingsBaseResource from "./OrgSettingsBaseResource";
/*
        "name": "default",
        "organizationName": "simple_report",
        "format": "CSV",
        "topic": "covid-19",
        "customerStatus": "active",
        "schemaName": "primedatainput/pdi-covid-19",
        "meta": {
            "version": 1,
            "createdBy": "local@test.com",
            "createdAt": "2021-12-23T22:20:12.262679Z"
        },
        "keys": null,
        "processingType": "sync"
*/

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
}
