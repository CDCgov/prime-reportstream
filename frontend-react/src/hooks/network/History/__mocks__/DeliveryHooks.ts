import * as DeliveryHooks from "../DeliveryHooks";
import { RSDelivery } from "../../../../network/api/History/Reports";

export const mockDeliveryListHook = jest.spyOn(DeliveryHooks, "useReportsList");
/** TEST UTILITY - generates `RSDelivery[]`, each with a unique `reportId` (starting from "0")
 * @param count {number} How many unique reports you want. */
export const deliveriesGenerator = (count: number) => {
    const deliveries: RSDelivery[] = [];
    for (let i = 0; i < count; i++) {
        deliveries.push(new RSDelivery({ reportId: `${i}` }));
    }
    return deliveries;
};
