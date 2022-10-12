import ActionResource from "../../../resources/ActionResource";
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
