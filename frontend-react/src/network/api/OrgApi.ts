import { Api, Endpoint } from "./Api";

export interface OrgMeta {
    version: number;
    createdBy: string;
    createdAt: string;
}

export interface Organization {
    name: string;
    description: string;
    jurisdiction: string;
    stateCode?: string;
    countyName?: string;
    filters?: Array<string>;
    meta: OrgMeta;
}

export class OrgApi extends Api {
    static baseUrl: string = "/api/settings/organizations";

    static list = (): Endpoint => {
        return OrgApi.generateEndpoint(this.baseUrl, this);
    };

    static detail = (oktaOrg: string): Endpoint => {
        return OrgApi.generateEndpoint(`${this.baseUrl}/${oktaOrg}`, this);
    };

    static senderDetail = (oktaOrg: string, sender: string): Endpoint => {
        return OrgApi.generateEndpoint(
            `${this.baseUrl}/${oktaOrg}/senders/${sender}`,
            this
        );
    };
}
