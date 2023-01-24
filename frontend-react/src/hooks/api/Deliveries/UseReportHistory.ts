import { deliveriesEndpoints } from "../../../config/api/deliveries";
import { useRSQuery } from "../UseRSQuery";

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query a single delivery
 * */
export const useReportHistory = (id: string) => {
    return useRSQuery(
        deliveriesEndpoints.reportHistory,
        {
            segments: {
                id,
            },
        },
        { enabled: !!id }
    );
};
