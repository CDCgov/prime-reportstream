import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback, useMemo, useState } from "react";
import { validate as uuidValidate } from "uuid";
import { deliveriesEndpoints, RSDeliveryHistoryResponse } from "../../../../config/endpoints/deliveries";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import useFilterManager, { FilterManagerDefaults } from "../../../filters/UseFilterManager/UseFilterManager";
import useAdminSafeOrganizationName from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { getDeliveriesHistory } = deliveriesEndpoints;

export enum DeliveriesDataAttr {
    REPORT_ID = "reportId",
    CREATED_AT = "createdAt",
    EXPIRES_AT = "expiresAt",
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

export type SearchFetcher<T> = (additionalParams?: SearchParams) => Promise<T[]>;

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DeliveriesDataAttr.CREATED_AT,
        locally: true,
    },
    pageDefaults: {
        size: 10,
    },
};

const useDeliveriesHistory = (initialService?: string) => {
    const [service, setService] = useState(initialService ?? "");
    const [searchTerm, setSearchTerm] = useState("");
    const { activeMembership, authorizedFetch } = useSessionContext();
    const adminSafeOrgName = useAdminSafeOrganizationName(activeMembership?.parsedName); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () => (service ? `${adminSafeOrgName}.${service}` : `${adminSafeOrgName}`),
        [adminSafeOrgName, service],
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
            return authorizedFetch<RSDeliveryHistoryResponse>(
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
    const { data, dataUpdatedAt } = useSuspenseQuery({
        queryKey: [getDeliveriesHistory.queryKey, activeMembership, orgAndService, filterManager, searchTerm],
        queryFn: memoizedDataFetch,
    });
    return {
        data,
        filterManager,
        searchTerm,
        setSearchTerm,
        setService,
        dataUpdatedAt,
    };
};

export default useDeliveriesHistory;
