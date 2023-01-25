import { useCallback } from "react";

import { deliveriesEndpoints } from "../../../config/api/deliveries";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";

// TODO: Use commented code instead
/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query a single delivery
 * */
export const useReportsDetail = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSDelivery>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(deliveriesEndpoints.reportDelivery, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        [deliveriesEndpoints.reportDelivery.meta.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { reportDetail: data };
};

// /** Hook consumes the ReportsApi "detail" endpoint and delivers the response
//  *
//  * @param id {string} Pass in the reportId to query a single delivery
//  * */
// export const useReportsDetail = (id: string) => {
//     return useRSQuery(
//         deliveriesEndpoints.reportDelivery,
//         {
//             segments: {
//                 id,
//             },
//         },
//         { enabled: !!id }
//     );
// };
