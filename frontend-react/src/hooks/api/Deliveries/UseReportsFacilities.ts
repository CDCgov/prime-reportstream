import { useCallback } from "react";

import { deliveriesEndpoints } from "../../../config/api/deliveries";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";

// TODO: Use commented code instead
/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
export const useReportsFacilities = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSFacility[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(deliveriesEndpoints.reportFacilities, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        [deliveriesEndpoints.reportFacilities.meta.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { reportFacilities: data };
};

// /** Hook consumes the ReportsApi "detail" endpoint and delivers the response
//  *
//  * @param id {string} Pass in the reportId to query for facilities on a report
//  * */
// export const useReportsFacilities = (id: string) => {
//     return useRSQuery(deliveriesEndpoints.reportFacilities, {
//         segments: {
//             id,
//         },
//     });
// };
