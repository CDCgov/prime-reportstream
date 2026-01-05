import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback, useMemo } from "react";

import {
    dataDashboardEndpoints,
    RSReceiverDeliveryResponse,
} from "../../../../config/endpoints/dataDashboard";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../filters/UseFilterManager/UseFilterManager";
import useAdminSafeOrganizationName from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { receiverDeliveries } = dataDashboardEndpoints;

export enum DeliveriesAttr {
    CREATED_AT = "created_at",
    ORDERING_PROVIDER = "ordering_provider",
    ORDERING_FACILITY = "ordering_facility",
    SUBMITTER = "submitter",
    REPORT_ID = "reportId",
}

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DeliveriesAttr.CREATED_AT,
        order: "DESC",
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
    const { activeMembership, authorizedFetch } = useSessionContext();
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

    const memoizedDataFetch = useCallback(() => {
        if (activeMembership?.parsedName) {
            return authorizedFetch<RSReceiverDeliveryResponse>(
                {
                    segments: { orgAndService },
                    data: {
                        sort: {
                            direction: sortDirection,
                            property: sortColumn,
                        },
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
                },
                receiverDeliveries,
            );
        }
        return null;
    }, [
        activeMembership?.parsedName,
        authorizedFetch,
        currentCursor,
        numResults,
        orgAndService,
        rangeFrom,
        rangeTo,
        sortColumn,
        sortDirection,
    ]);
    const { data } = useSuspenseQuery({
        queryKey: [
            receiverDeliveries.queryKey,
            activeMembership,
            orgAndService,
            filterManager,
        ],
        queryFn: memoizedDataFetch,
    });

    return { data, filterManager, isLoading: false };
}
