import { deliveriesEndpoints } from "../../../config/api/deliveries";
import { useRSQuery } from "../UseRSQuery";

// TODO: Use commented code instead
/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
export const useReportFacilities = (id: string) => {
    return useRSQuery(deliveriesEndpoints.reportFacilities, {
        segments: {
            id,
        },
    });
};
