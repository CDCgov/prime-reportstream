import { Api } from "./Api";

export interface Sender {
    name: string;
    organizationName: string;
    format: "CSV" | "HL7";
    topic: string;
    customerStatus: "inactive" | "testing" | "active";
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

export interface Organization {
    name: string;
    description: string;
    jurisdiction: string;
    stateCode: string;
    countyName: string;
    filters: Array<OrgFilters>;
    meta: OrgMeta;
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

export const orgApi = new OrgApi("settings/organizations");
