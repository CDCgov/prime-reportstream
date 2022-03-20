import { primeApiConfig } from "../config";

import { Api } from "./Api";

export interface Sender {
    name: string;
    organizationName: string;
    format: "CSV" | "HL7";
    topic: string;
    customerStatus: string; // Narrow this down to it's possible values
    schemaName: string;
}

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
        this.name = name;
        this.description = description;
        this.jurisdiction = jurisdiction;
        this.stateCode = stateCode;
        this.countyName = countyName;
        this.filters = filters;
        this.meta = meta;
    }

    name: string = "";
    description: string = "";
    jurisdiction: string = "";
    stateCode: string = "";
    countyName: string = "";
    filters: Array<OrgFilters> = [];
    meta: OrgMeta = {
        version: -1,
        createdBy: "",
        createdAt: "",
    };
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
        return this.configure<Sender>({
            method: "GET",
            url: `${this.basePath}/${oktaGroup}/senders/${sender}`,
        });
    };
}

export const orgApi = new OrgApi(primeApiConfig, "settings/organizations");
