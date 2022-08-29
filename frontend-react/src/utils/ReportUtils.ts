import { RSDelivery } from "../network/api/History/Reports";
import ReportResource from "../resources/ReportResource";

// This function returns a list of unique senders of any ReportResource[]
export function getUniqueReceiverSvc(
    reports: RSDelivery[] | ReportResource[] | undefined
): Set<string> | string[] {
    if (!reports) return [];
    return new Set(reports.map((r) => r.receivingOrgSvc));
}
