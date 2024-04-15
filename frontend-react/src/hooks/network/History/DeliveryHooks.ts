import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback, useMemo, useState } from "react";

import {
    deliveriesEndpoints,
    RSDelivery,
    RSFacility,
} from "../../../config/endpoints/deliveries";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetch";
import { useSessionContext } from "../../../contexts/Session";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../filters/UseFilterManager";
import {
    Organizations,
    useAdminSafeOrganizationName,
} from "../../UseAdminSafeOrganizationName";

const { getOrgDeliveries, getDeliveryDetails, getDeliveryFacilities } =
    deliveriesEndpoints;

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

/** Hook consumes the ReportsApi "list" endpoint and delivers the response
 *
 * @param service {string} the chosen receiver service (e.x. `elr-secondary`)
 * */
const useOrgDeliveries = (initialService?: string) => {
    const [service, setService] = useState(
        initialService ? initialService : "",
    );
    const { activeMembership } = useSessionContext();
    const authorizedFetch = useAuthorizedFetch();

    const adminSafeOrgName = useAdminSafeOrganizationName(
        activeMembership?.parsedName,
    ); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () =>
            service ? `${adminSafeOrgName}.${service}` : `${adminSafeOrgName}`,
        [adminSafeOrgName, service],
    );

    // Pagination and filter props
    const filterManager = useFilterManager(filterManagerDefaults);
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    const fetchResults = useCallback(
        (currentCursor: string, numResults: number, additionalParams = {}) => {
            if (activeMembership?.parsedName === Organizations.PRIMEADMINS) {
                return Promise.resolve<RSDelivery[]>([]);
            }

            const params = {
                sortdir: sortOrder,
                cursor: currentCursor,
                since: rangeFrom,
                until: rangeTo,
                pageSize: numResults,
                receivingOrgSvcStatus: "ACTIVE,TESTING",
                ...additionalParams,
            };
            // Basically, if there are search parameters present, ignore the
            // specifically chosen Receiver, so that the search can be across
            // ALL Receivers
            const segmentParam = Object.keys(additionalParams).length
                ? adminSafeOrgName
                : orgAndService;

            return authorizedFetch(getOrgDeliveries, {
                segments: {
                    orgAndService: segmentParam,
                },
                params,
            }) as unknown as Promise<RSDelivery[]>;
        },
        [
            activeMembership?.parsedName,
            sortOrder,
            rangeFrom,
            rangeTo,
            adminSafeOrgName,
            orgAndService,
            authorizedFetch,
        ],
    );

    return { fetchResults, filterManager, setService };
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
    return useSuspenseQuery({
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        queryKey: [getDeliveryDetails.queryKey, id],
        queryFn: memoizedDataFetch,
    });
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
const useReportsFacilities = (id: string) => {
    const authorizedFetch = useAuthorizedFetch<RSFacility[]>();
    const memoizedDataFetch = useCallback(() => {
        if (id) {
            return authorizedFetch(getDeliveryFacilities, {
                segments: {
                    id: id,
                },
            });
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

export { useOrgDeliveries, useReportsDetail, useReportsFacilities };
