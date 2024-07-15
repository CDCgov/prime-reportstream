import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback, useMemo, useState } from "react";
import { validate as uuidValidate } from "uuid";
import { RSReceiverDeliveryResponse } from "../../../../config/endpoints/dataDashboard";
import { deliveriesEndpoints } from "../../../../config/endpoints/deliveries";
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
    const [service, setService] = useState(initialService ?? "");
    const { activeMembership, authorizedFetch } = useSessionContext();
    const adminSafeOrgName = useAdminSafeOrganizationName(
        activeMembership?.parsedName,
    ); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () =>
            service ? `${adminSafeOrgName}.${service}` : `${adminSafeOrgName}`,
        [adminSafeOrgName, service],
    );
    const [searchTerm, setSearchTerm] = useState("");
    // Pagination and filter props
    const filterManager = useFilterManager(filterManagerDefaults);
    const sortColumn = filterManager.sortSettings.column;
    const sortDirection = filterManager.sortSettings.order;
    const currentCursor = filterManager.pageSettings.currentPage;
    const numResults = filterManager.pageSettings.size;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    const memoizedDataFetch = useCallback(() => {
        // Search terms can either be fileName string or a UUID,
        // and we need to know since we have to query the API by
        // that specific query param. All reportId(s) are UUIDs, so
        // if the searchTerm is a UUID, assume reportId, otherwise
        // assume fileName
        const searchParam = searchTerm
            ? uuidValidate(searchTerm)
                ? { reportId: searchTerm }
                : { fileName: searchTerm }
            : {};
        const params = {
            receivingOrgSvcStatus: "ACTIVE,TESTING",
            ...searchParam,
        };
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
                    params,
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
        searchTerm,
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

    return {
        data,
        filterManager,
        searchTerm,
        setSearchTerm,
        setService,
        isLoading: false,
    };
};

export default useDeliveriesHistory;
