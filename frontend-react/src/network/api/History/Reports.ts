import { API } from "../NewApi";
import ActionResource from "../../../resources/ActionResource";
/**
 * @deprecated Replaced by deliveries endpoint resource
 */
class RSReport {}
/** A class representing a Delivery object from the API */
export class RSDelivery {
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

/**
 * Contains the API information for the ReportStream Deliveries API
 * 1. Resource: {@link RSDelivery}
 * 2. Endpoints:
 *      <ul>
 *          <li>"list" -> A list of deliveries for an organization and service (i.e. md-phd.elr-secondary)</li>
 *          <li>"detail" -> A single delivery item with more detail, including file content for download</li>
 *      </ul>
 */
export const DeliveryApi = new API(RSDelivery, "/api/waters/org")
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
