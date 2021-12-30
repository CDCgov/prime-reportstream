import AuthResource from "./AuthResource";
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

interface MetaData {
    version: number;
    createdBy: string;
    createdAt: Date;
}

export default class OrgSenderSettingsResource extends AuthResource {
    readonly name: string = "";
    readonly organizationName: string = "";
    readonly format: string = "";
    readonly topic: string = "";
    readonly customerStatus: string = "";
    readonly schemaName: string = "";
    readonly keys: string[] = [];
    readonly processingType: string = "";
    readonly meta: MetaData[] = [];

    pk() {
        return this.name;
    }

    static get key() {
        return "name";
    }

    static listUrl(params: { orgname: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/senders`;
    }

    static url(params: { orgname: string; sendername: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/senders/${params.sendername}`;
    }
}
