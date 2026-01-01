import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback, useMemo } from "react";

import {
    dataDashboardEndpoints,
    RSReceiverSubmitterResponse,
} from "../../../../config/endpoints/dataDashboard";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../filters/UseFilterManager/UseFilterManager";
import useAdminSafeOrganizationName from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { receiverSubmitters } = dataDashboardEndpoints;

export enum DeliveriesAttr {
    NAME = "name",
    REPORT_DATE = "first_report_date",
    FACILITY_TYPE = "type",
    LOCATION = "location",
    TEST_RESULT_COUNT = "test_result_count",
}

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DeliveriesAttr.NAME,
        order: "ASC",
        locally: true,
    },
    pageDefaults: {
        size: 10,
        currentPage: 1,
    },
};

/** Returns a list of all the provider facilities who have submitted to a receiver
 *
 * @param serviceName {string} the selected receiver service (e.x. elr-secondary)
 * */
export default function useReceiverSubmitters(serviceName?: string) {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const adminSafeOrgName = useAdminSafeOrganizationName(
        activeMembership?.parsedName,
    ); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () =>
            adminSafeOrgName ? `${adminSafeOrgName}.${serviceName}` : undefined,
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
        if (orgAndService) {
            return authorizedFetch<RSReceiverSubmitterResponse>(
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
                receiverSubmitters,
            );
        }
        return null;
    }, [
        authorizedFetch,
        currentCursor,
        numResults,
        orgAndService,
        rangeFrom,
        rangeTo,
        sortColumn,
        sortDirection,
    ]);
    return {
        ...useSuspenseQuery({
            queryKey: [
                receiverSubmitters.queryKey,
                activeMembership,
                orgAndService,
                filterManager,
            ],
            queryFn: memoizedDataFetch,
        }),
        filterManager,
    };
}
