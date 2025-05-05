import { RSDelivery } from "../config/endpoints/deliveries";
import ActionResource from "../resources/ActionResource";

/**
 * @todo Remove once refactored out of Report download call (when RSDelivery.content exists)
 * @deprecated For compile-time type checks while #5892 is worked on
 */
export interface RSReportInterface {
    batchReadyAt: number;
    via: string;
    positive: number;
    reportItemCount: number;
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

function extractService(receiver: string) {
    const service = receiver.split(".")?.[1];
    if (service === undefined)
        throw new Error(`Receiver name ${receiver} lacks a service definition. Services are deliminated
    with periods. ex: xx_phd.service`);
    return service ?? "";
}

// This function returns a list of unique senders of any ReportResource[]
export function getUniqueReceiverSvc(reports: RSDelivery[] | undefined): Set<string> | string[] {
    if (!reports) return [];
    return new Set(reports.map((r) => extractService(r.receiver)));
}
