import ActionResource from "./ActionResource";
import AuthResource from "./AuthResource";

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

    pk() {
        return this.reportId;
    }

    static urlRoot = `${process.env.REACT_APP_BACKEND_URL}/api/history/report`;
}
