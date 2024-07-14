import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback, useMemo } from "react";
import {
    deliveriesEndpoints,
    RSDelivery,
} from "../../../../config/endpoints/deliveries";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../filters/UseFilterManager/UseFilterManager";
import useAdminSafeOrganizationName from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { getDeliveriesHistory } = deliveriesEndpoints;

export enum DeliveriesDataAttr {
    REPORT_ID = "reportId",
    BATCH_READY = "batchReadyAt",
    EXPIRES = "expires",
    ITEM_COUNT = "reportItemCount",
    FILE_NAME = "fileName",
    RECEIVER = "receiver",
}

export type SearchParams =
    | {
          reportId: string;
      }
    | {
          fileName: string;
      };

export type SearchFetcher<T> = (
    additionalParams?: SearchParams,
) => Promise<T[]>;

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DeliveriesDataAttr.BATCH_READY,
        locally: true,
    },
    pageDefaults: {
        size: 10,
    },
};

const useDeliveriesHistory = (initialService?: string) => {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const adminSafeOrgName = useAdminSafeOrganizationName(
        activeMembership?.parsedName,
    ); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () => `${adminSafeOrgName}.${initialService}`,
        [adminSafeOrgName, initialService],
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
            return authorizedFetch<RSDelivery[]>(
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
                getDeliveriesHistory,
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
            getDeliveriesHistory.queryKey,
            activeMembership,
            orgAndService,
            filterManager,
        ],
        queryFn: memoizedDataFetch,
    });

    return { data, filterManager, isLoading: false };
};

export default useDeliveriesHistory;
