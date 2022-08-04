import {
    DeliveryApi,
    DeliveryDetailParams,
    DeliveryListParams,
    RSDelivery,
} from "../../../network/api/History/Reports";
import { useApiEndpoint } from "../UseApiEndpoint";

/** Hook consumes the ReportsApi "list" endpoint and delivers the response
 *
 * @param org {string} the `parsedName` of user's active membership
 * @param service {string} the chosen receiver service (e.x. `elr-secondary`)
 * @returns {BasicAPIResponse<RSReportInterface[]>}
 * */
const useReportsList = (org?: string, service?: string) =>
    useApiEndpoint<DeliveryListParams, RSDelivery[]>(
        DeliveryApi,
        "list",
        "GET",
        { orgAndService: `${org}.${service}` },
        { requireTrigger: true }
    );

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param deliveryId {number} Pass in the reportId to query the API
 * @returns {BasicAPIResponse<RSReportInterface>}
 * */
const useReportsDetail = (deliveryId: number) =>
    useApiEndpoint<DeliveryDetailParams, RSDelivery>(
        DeliveryApi,
        "detail",
        "GET",
        {
            deliveryId: deliveryId,
        }
    );

export { useReportsList, useReportsDetail };
