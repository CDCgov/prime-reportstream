import { API } from "../NewApi";
import ActionResource from "../../../resources/ActionResource";
/**
 * @todo Remove once refactored out of Report download call (when RSDelivery.content exists)
 * @deprecated For compile-time type checks while #5892 is worked on
 */
export interface RSReportInterface {
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

    constructor(reportId: string) {
        this.reportId = reportId;
    }
    mockResource(reportId: string) {
        return new RSDelivery(reportId);
    }
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
export const DeliveryApi = new API(RSDelivery, "/api/waters")
    .addEndpoint("list", "/org/:orgAndService/deliveries", ["GET"])
    .addEndpoint("detail", "/report/:id/delivery", ["GET"]);

export interface DeliveryListParams {
    orgAndService: string;
}
export interface DeliveryDetailParams {
    id: string;
}
