import { RSDelivery } from "../config/endpoints/deliveries";

function extractService(receiver: string) {
    const service = receiver.split(".")?.[1];
    if (service === undefined)
        console.warn(`Receiver name ${receiver} lacks a service definition. Services are deliminated
    with periods. ex: xx_phd.service`);
    return service ? service : "";
}
// This function returns a list of unique senders of any ReportResource[]
export function getUniqueReceiverSvc(
    reports: RSDelivery[] | undefined,
): Set<string> | string[] {
    if (!reports) return [];
    return new Set(reports.map((r) => extractService(r.receiver)));
}
