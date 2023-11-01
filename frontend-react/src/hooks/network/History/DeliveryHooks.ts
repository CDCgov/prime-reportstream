import { useCallback, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";

import {
    Organizations,
    useAdminSafeOrganizationName,
} from "../../UseAdminSafeOrganizationName";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import {
    deliveriesEndpoints,
    RSDelivery,
    RSFacility,
} from "../../../config/endpoints/deliveries";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/SessionContext";

const { getOrgDeliveries, getDeliveryDetails, getDeliveryFacilities } =
    deliveriesEndpoints;

export enum DeliveriesDataAttr {
    REPORT_ID = "reportId",
    BATCH_READY = "batchReadyAt",
    EXPIRES = "expires",
    ITEM_COUNT = "reportItemCount",
    FILE_NAME = "fileName",
}

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DeliveriesDataAttr.BATCH_READY,
        locally: true,
    },
    pageDefaults: {
        size: 10,
    },
};

/** Hook consumes the ReportsApi "list" endpoint and delivers the response
 *
 * @param service {string} the chosen receiver service (e.x. `elr-secondary`)
 * */
const useOrgDeliveries = (service?: string) => {
    const { activeMembership } = useSessionContext();
    const authorizedFetch = useAuthorizedFetch();

    const adminSafeOrgName = useAdminSafeOrganizationName(
        activeMembership?.parsedName,
    ); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () => `${adminSafeOrgName}.${service}`,
        [adminSafeOrgName, service],
    );

    const filterManager = useFilterManager(filterManagerDefaults);
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    const fetchResults = useCallback(
        (currentCursor: string, numResults: number) => {
            // HACK: return empty results if requesting as an admin
            if (activeMembership?.parsedName === Organizations.PRIMEADMINS) {
                return Promise.resolve<RSDelivery[]>([]);
            }

            return authorizedFetch(getOrgDeliveries, {
                segments: {
                    orgAndService,
                },
                params: {
                    sortdir: sortOrder,
                    cursor: currentCursor,
                    since: rangeFrom,
                    until: rangeTo,
                    pageSize: numResults,
                },
            }) as unknown as Promise<RSDelivery[]>;
        },
        [
            activeMembership?.parsedName,
            authorizedFetch,
            orgAndService,
            sortOrder,
            rangeFrom,
            rangeTo,
        ],
    );

    return { fetchResults, filterManager };
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query a single delivery
 * */
const useReportsDetail = (id: string) => {
    const authorizedFetch = useAuthorizedFetch<RSDelivery>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getDeliveryDetails, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id],
    );
    return useQuery({
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        queryKey: [getDeliveryDetails.queryKey, id],
        queryFn: memoizedDataFetch,
        enabled: !!id,
    });
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
const useReportsFacilities = (id: string) => {
    const authorizedFetch = useAuthorizedFetch<RSFacility[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getDeliveryFacilities, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id],
    );
    return useQuery({
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        queryKey: [getDeliveryFacilities.queryKey, id],
        queryFn: memoizedDataFetch,
        enabled: !!id,
    });
};

export { useOrgDeliveries, useReportsDetail, useReportsFacilities };
