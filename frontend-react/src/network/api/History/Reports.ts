import { API } from "../NewApi";
import ActionResource from "../../../resources/ActionResource";

class RSReport {}
/* For compile-time type checks while #5892 is worked on */
interface TempRSReport {
    sent: number;
    via: string;
    positive: number;
    total: number;
    fileType: string;
    type: string;
    reportId: string;
    expires: number;
    sendingOrg: string;
    receivingOrg: string;
    receivingOrgSvc: string;
    facilities: string[];
    actions: ActionResource[];
    displayName: string;
    content: string;
    fileName: string;
    mimeType: string;
}
const ReportsApi = new API(RSReport, "/api/history/report");
ReportsApi.addEndpoint("list", "", ["GET"]);

export default ReportsApi;
export { RSReport };
export type { TempRSReport };
