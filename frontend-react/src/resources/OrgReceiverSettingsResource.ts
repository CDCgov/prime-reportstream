import AuthResource from "./AuthResource";
/*
        "name": "giang",
        "organizationName": "waters",
        "topic": "covid-19",
        "customerStatus": "active",
        "translation": {
            "schemaName": "waters/waters-covid-19",
            "format": "CSV",
            "defaults": {},
            "nameFormat": "STANDARD",
            "receivingOrganization": null,
            "type": "CUSTOM"
        },
        "jurisdictionalFilter": [
            "hasAtLeastOneOf(waters_submitter, sender_id)",
            "orEquals(patient_state, GH, ordering_facility_state, GH)"
        ],
        "qualityFilter": [
            "allowAll()"
        ],
        "routingFilter": [],
        "processingModeFilter": [],
        "reverseTheQualityFilter": false,
        "deidentify": true,
        "timing": {
            "operation": "MERGE",
            "numberPerDay": 1440,
            "initialTime": "00:00",
            "timeZone": "EASTERN",
            "maxReportCount": 10000
        },
        "description": "",
        "transport": {
            "host": "sftp",
            "port": "22",
            "filePath": "./upload",
            "credentialName": "DEFAULT-SFTP",
            "type": "SFTP"
        },
        "meta": {
            "version": 0,
            "createdBy": "local@test.com",
            "createdAt": "2021-12-15T23:52:17.669824Z"
        },
        "externalName": null
    }
*/

interface MetaData {
    version: number;
    createdBy: string;
    createdAt: Date;
}

interface Translation {
    schemaName: string;
    format: string;
    nameFormat: string;
    receivingOrganization: string;
    type: string;
}

interface Timing {
    operation: string;
    numberPerDay: number;
    initialTime: string;
    timeZone: string;
    maxReportCount: number;
}

interface Transport {
    type: string; //can be of type "SFTP", "EMAIL", "REDOX" or "BLOBSTORE"

    // type = "SFTP"
    host: string;
    port: string;
    filePath: string;
    credentialName: string | null;

    // type = "EMAIL"
    addresses: string[];
    from: string;

    // type = "REDOX"
    apiKey: string;
    baseUrl: string;

    // type = "BLOBSTORAGE"
    storageName: string;
    containerName: string;
}

export default class OrgReceiverSettingsResource extends AuthResource {
    readonly name: string = "";
    readonly organizationName: string = "";
    readonly topic: string = "";
    readonly customerStatus: string = "";
    readonly schemaName: string = "";
    readonly processingType: string = "";
    readonly meta: MetaData[] = [];
    readonly translation: Translation[] = [];
    readonly description: string = "";
    readonly jurisdictionalFilter: string[] = [];
    readonly qualityFilter: string[] = [];
    readonly routingFilter: string[] = [];
    readonly processingModeFilter: string[] = [];
    readonly reverseTheQualityFilter: boolean = false;
    readonly deidentify: boolean = false;
    readonly timing: Timing[] = [];
    readonly transport: Transport[] = [];
    readonly externalName: string = "";

    pk() {
        return this.name;
    }

    static get key() {
        return "name";
    }

    static listUrl(params: { orgname: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/receivers`;
    }

    static url(params: { orgname: string; receivername: string }): string {
        return `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/${params.orgname}/receivers/${params.receivername}`;
    }
}
