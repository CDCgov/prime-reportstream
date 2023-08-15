import { useCallback, useMemo } from "react";

import {
    RSReceiverDeliveryResponse,
    dataDashboardEndpoints,
} from "../../../config/endpoints/dataDashboard";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useAdminSafeOrganizationName } from "../../UseAdminSafeOrganizationName";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../filters/UseFilterManager";

const { receiverDeliveries } = dataDashboardEndpoints;

export enum DeliveriesAttr {
    CREATED_AT = "createdAt",
    ORDERING_PROVIDER = "orderingProvider",
    ORDERING_FACILITY = "orderingFacility",
    SUBMITTER = "submitter",
    REPORT_ID = "reportId",
}

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DeliveriesAttr.CREATED_AT,
        order: "ASC",
        locally: true,
    },
    pageDefaults: {
        size: 10,
        currentPage: 1,
    },
};

/** Returns a list of reports that have been delivered to the receiver
 *
 * @param serviceName {string} the selected receiver service (e.x. elr-secondary)
 * */
export default function useReceiverDeliveries(serviceName?: string) {
    const { activeMembership } = useSessionContext();
    const adminSafeOrgName = useAdminSafeOrganizationName(
        activeMembership?.parsedName,
    ); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () => `${adminSafeOrgName}.${serviceName}`,
        [adminSafeOrgName, serviceName],
    );

    // Pagination and filter props
    const filterManager = useFilterManager(filterManagerDefaults);
    const sortColumn = filterManager.sortSettings.column;
    const sortDirection = filterManager.sortSettings.order;
    const currentCursor = filterManager.pageSettings.currentPage;
    const numResults = filterManager.pageSettings.size;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSReceiverDeliveryResponse>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(receiverDeliveries, {
                segments: { orgAndService },
                data: {
                    sort: { direction: sortDirection, property: sortColumn },
                    pagination: { page: currentCursor, limit: numResults },
                    filters: [
                        {
                            filterName: "SINCE",
                            value: rangeFrom,
                        },
                        {
                            filterName: "UNTIL",
                            value: rangeTo,
                        },
                    ],
                },
            }),
        [
            authorizedFetch,
            currentCursor,
            numResults,
            orgAndService,
            rangeFrom,
            rangeTo,
            sortColumn,
            sortDirection,
        ],
    );
    const { data, isLoading } = rsUseQuery(
        [
            receiverDeliveries.queryKey,
            activeMembership,
            orgAndService,
            filterManager,
        ],
        memoizedDataFetch,
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
        },
    );

    return { data, filterManager, isLoading };
}
