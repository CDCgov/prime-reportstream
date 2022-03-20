import { primeApiConfig } from "../config";

import { Api } from "./Api";

interface OrgFilters {
    topic: string;
    jurisdictionalFilter: Array<string>;
    qualityFilter?: Array<string>;
    routingFilter?: Array<string>;
    processingModeFilter?: Array<string>;
}

interface OrgMeta {
    version: number;
    createdBy: string;
    createdAt: string;
}

export class Organization {
    constructor(
        name: string,
        description: string,
        jurisdiction: string,
        stateCode: string,
        countyName: string,
        filters: Array<OrgFilters>,
        meta: OrgMeta
    ) {
        name = "";
        description = "";
        jurisdiction = "";
        stateCode = "";
        countyName = "";
        filters = [];
        meta = {
            version: -1,
            createdBy: "",
            createdAt: "",
        };
    }
}

class OrgApi extends Api {
    getOrgList = () => {
        return this.configure<Organization[]>({
            method: "GET",
            url: this.basePath,
        });
    };

    getOrgDetail = (oktaGroup: string) => {
        return this.configure<Organization>({
            method: "GET",
            url: `${this.basePath}/${oktaGroup}`,
        });
    };

    getSenderDetail = (oktaGroup: string, sender: string) => {
        return this.configure<Organization>({
            method: "GET",
            url: `${this.basePath}/${oktaGroup}/senders/${sender}`,
        });
    };
}

export const orgApi = new OrgApi(primeApiConfig, "settings/organizations");
