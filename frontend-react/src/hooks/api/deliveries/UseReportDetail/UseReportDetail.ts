import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";
import { deliveriesEndpoints, RSDelivery } from "../../../../config/endpoints/deliveries";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { getDeliveryDetails } = deliveriesEndpoints;

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query a single delivery
 * */
const useReportsDetail = (id: string) => {
    const { authorizedFetch } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch<RSDelivery>(
                {
                    segments: {
                        id: id,
                    },
                },
                getDeliveryDetails,
            ),
        [authorizedFetch, id],
    );
    return useSuspenseQuery({
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        queryKey: [getDeliveryDetails.queryKey, id],
        queryFn: memoizedDataFetch,
    });
};

export default useReportsDetail;
