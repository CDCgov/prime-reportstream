import { API } from "../NewApi";
import ActionResource from "../../../resources/ActionResource";
/**
 * @deprecated For compile-time type checks while #5892 is worked on
 */
class RSReport {}
class RSDelivery {
    deliveryId: number = -1;
    sent: string = "";
    expires: string = "";
    receivingOrg: string = "";
    receivingOrgSvc: string = "";
    reportId: string = "";
    topic: string = "";
    reportItemCount: number = -1;
    fileName: string = "";
    fileType: string = "";
    externalName: string = "";
}
/**
 * @deprecated For compile-time type checks while #5892 is worked on
 */
interface RSReportInterface {
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
/**
 * @deprecated Working to remove this and implement DeliveryApi
 */
const ReportsApi = new API(RSReport, "/api/history/report")
    .addEndpoint("list", "", ["GET"])
    .addEndpoint("detail", "/:id", ["GET"]);

export interface ReportDetailParams {
    id: string;
}

export const DeliveryApi = new API(RSDelivery, "/api/waters")
    .addEndpoint("list", "/:orgAndService/deliveries", ["GET"])
    .addEndpoint("detail", "/delivery/:deliveryId/detail", ["GET"]);

export interface DeliveryListParams {
    orgAndService: string;
}
export interface DeliveryDetailParams {
    deliveryId: number;
}

export default ReportsApi;
export { RSReport };
export type { RSReportInterface };
