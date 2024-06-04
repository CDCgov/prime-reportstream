import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";
import {
    deliveriesEndpoints,
    RSFacility,
} from "../../../../config/endpoints/deliveries";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { getDeliveryFacilities } = deliveriesEndpoints;

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
const useReportsFacilities = (id: string) => {
    const { authorizedFetch } = useSessionContext();
    const memoizedDataFetch = useCallback(() => {
        if (id) {
            return authorizedFetch<RSFacility[]>(
                {
                    segments: {
                        id: id,
                    },
                },
                getDeliveryFacilities,
            );
        }
        return null;
    }, [authorizedFetch, id]);
    return useSuspenseQuery({
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        queryKey: [getDeliveryFacilities.queryKey, id],
        queryFn: memoizedDataFetch,
    });
};

export default useReportsFacilities;
