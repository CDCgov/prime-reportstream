import useEndpoint from "../../hooks/network/UseEndpoint";

import { Api } from "./Api";
import { API } from "./NewApi";

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

export class RSSender {
    name: string;
    organizationName: string;
    format: "CSV" | "HL7";
    topic: string;
    customerStatus: "inactive" | "testing" | "active";
    schemaName: string;

    constructor(
        name: string,
        organizationName: string,
        format: "CSV" | "HL7",
        topic: string,
        customerStatus: "inactive" | "testing" | "active",
        schemaName: string
    ) {
        this.name = name;
        this.organizationName = organizationName;
        this.format = format;
        this.topic = topic;
        this.customerStatus = customerStatus;
        this.schemaName = schemaName;
    }
}

class RSOrganization {
    name: string;
    description: string;
    jurisdiction: string;
    stateCode: string;
    countyName: string;
    filters: Array<OrgFilters>;
    meta: OrgMeta;

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
}

const OrgSettingsApi: API<RSOrganization> = {
    resource: RSOrganization,
    baseUrl: "/api/settings/organizations",
    endpoints: new Map(),
};
OrgSettingsApi.endpoints.set("getOrgList", {
    url: "/",
    methods: ["GET"],
});
OrgSettingsApi.endpoints.set("getOrgDetail", {
    url: "/:org",
    methods: ["GET"],
});

const SenderApi: API<RSSender> = {
    resource: RSSender,
    baseUrl: "/api/settings/organizations",
    endpoints: new Map(),
};
SenderApi.endpoints.set("getSenderDetail", {
    url: "/:org/senders/:sender",
    methods: ["GET"],
});

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

const useGetSenderDetail = (org: string, sender: string) =>
    useEndpoint<RSSender, { org: string; sender: string }>(
        SenderApi,
        "getSenderDetail",
        "GET",
        { org, sender }
    ) as {
        data: RSSender | undefined;
    };

export const orgApi = new OrgApi("settings/organizations");
export { useGetSenderDetail };
