import { Api } from "./Api";
import { Endpoint } from "../NetworkTypes";

/* 
    Using classes allows us to keep some easy defaults when constructing
    an object from a JSON response. This is meant to mimic the behavior of
    rest-hooks defaults.
*/
export class Report {
    sent: number = -1;
    via: string = "";
    total: number = -1;
    fileType: string = "";
    type: string = "";
    reportId: string = "";
    expires: number = -1;
    sendingOrg: string = "";
    receivingOrg: string = "";
    receivingOrgSvc: string = "";
    facilities: string[] = [];
    actions: Action[] = [];
    displayName: string = "";
    content: string = "";
    fieldName: string = "";
    mimeType: string = "";
}

export class Action {
    date: string = "";
    user: string = "";
    action: string | undefined;
}

export class HistoryApi extends Api {
    static baseUrl: string = "/api/history/report";

    static list = (): Endpoint => {
        return HistoryApi.generateEndpoint(this.baseUrl, this);
    };

    static detail = (reportId: string): Endpoint => {
        return HistoryApi.generateEndpoint(`${this.baseUrl}/${reportId}`, this);
    };
}