import { Endpoint } from "../api/Api";

import { Api } from "./Api";

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

/* 
    Typically the only things needing set in a class extending our Api class
    will be an override to the baseUrl. You could override the config, as well,
    in cases where alternative/additional headers are necessary.
*/
export class HistoryApi extends Api {
    /* MEMBER OVERRIDES */
    static baseUrl: string = "/api/history/report";

    /* ENDPOINTS */
    static list = (): Endpoint => {
        return HistoryApi.generateEndpoint(this.baseUrl, this);
    };

    static detail = (reportId: string): Endpoint => {
        return HistoryApi.generateEndpoint(`${this.baseUrl}/${reportId}`, this);
    };

    /* FOR TESTING ONLY */
    static testResponse = (many: number): Report | Report[] => {
        if (many === 1) return new Report();

        const responseArray: Report[] = [];
        while (many > 0) {
            responseArray[many] = new Report();
            many -= 1;
        }
        return responseArray;
    };
}
