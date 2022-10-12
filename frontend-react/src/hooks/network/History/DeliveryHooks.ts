import { useCallback, useMemo } from "react";

import { useAdminSafeOrgName } from "../UseMemoizedConfig";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { deliveriesEndpoints } from "../../../config/endpoints/deliveries";
import { RSDelivery } from "../../../network/api/History/Reports";

const { getOrgDeliveries, getDeliveryDetails, getDeliveryFacilities } =
    deliveriesEndpoints;

/** Hook consumes the ReportsApi "list" endpoint and delivers the response
 *
 * @param org {string} the `parsedName` of user's active membership
 * @param service {string} the chosen receiver service (e.x. `elr-secondary`)
 * @returns {BasicAPIResponse<RSReportInterface[]>}
 * */
const useOrgDeliveries = (org?: string, service?: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSDelivery[]>();
    const adminSafeOrgName = useAdminSafeOrgName(org); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () => `${adminSafeOrgName}.${service}`,
        [adminSafeOrgName, service]
    ); // ex: xx-phd.elr
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getOrgDeliveries, {
                segments: {
                    orgAndService,
                },
            }),
        [authorizedFetch, orgAndService]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when swapping services
        [getOrgDeliveries.queryKey, orgAndService],
        memoizedDataFetch,
        { enabled: !!service }
    );
    return { serviceReportsList: data };
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string | number} Pass in the reportId OR deliveryId to query a single delivery
 * @returns {detailedReport: RSDelivery}
 * */
const useReportsDetail = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSDelivery>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getDeliveryDetails, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        [getDeliveryDetails.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { reportDetail: data };
};

export { useOrgDeliveries, useReportsDetail };
