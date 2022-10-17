import config from "../config";

import ActionResource from "./ActionResource";
import AuthResource from "./AuthResource";

const { RS_API_URL } = config;

export default class ReportResource extends AuthResource {
    readonly sent: number = 1;
    readonly via: string = "";
    // readonly positive: number = 1;
    readonly total: number = 1;
    readonly fileType: string = "";
    readonly type: string = "";
    readonly reportId: string = "";
    readonly expires: number = 1;
    readonly sendingOrg: string = "";
    readonly receivingOrg: string = "";
    readonly receivingOrgSvc: string = "";
    readonly facilities: string[] = [];
    readonly actions: ActionResource[] = [];
    readonly displayName: string = "";
    readonly content: string = "";
    readonly fileName: string = "";
    readonly mimeType: string = "";

    constructor(
        reportId?: string,
        sent?: number,
        expires?: number,
        total?: number,
        fileType?: string
    ) {
        super();
        this.reportId = reportId || "";
        this.sent = sent || 0;
        this.expires = expires || 0;
        this.total = total || 0;
        this.fileType = fileType || "";
    }

    pk() {
        return this.reportId;
    }

    static urlRoot = `${RS_API_URL}/api/history/report`;
}
