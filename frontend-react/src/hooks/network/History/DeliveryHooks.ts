import { useCallback, useMemo } from "react";
import { AccessToken } from "@okta/okta-auth-js";

import {
    Organizations,
    useAdminSafeOrganizationName,
} from "../../UseAdminSafeOrganizationName";
import { useAuthorizedFetch } from "../../../contexts/AuthorizedFetchContext";
import { deliveriesEndpoints } from "../../../config/api/deliveries";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useCreateFetch } from "../../UseCreateFetch";
import { MembershipSettings } from "../../UseOktaMemberships";

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
    const { oktaToken, activeMembership } = useSessionContext();
    // Using this hook rather than the provided one through AuthFetchProvider because of a hard-to-isolate
    // infinite refresh bug. The authorizedFetch function would trigger endless updates and thus re-fetch
    // endlessly.
    const generateFetcher = useCreateFetch(
        oktaToken as AccessToken,
        activeMembership as MembershipSettings
    );

    const adminSafeOrgName = useAdminSafeOrganizationName(
        activeMembership?.parsedName
    ); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () => `${adminSafeOrgName}.${service}`,
        [adminSafeOrgName, service]
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

            const fetcher = generateFetcher();
            return fetcher(deliveriesEndpoints.orgAndServiceDeliveries, {
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
            orgAndService,
            sortOrder,
            generateFetcher,
            rangeFrom,
            rangeTo,
            activeMembership?.parsedName,
        ]
    );

    return { fetchResults, filterManager };
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query a single delivery
 * */
const useReportsDetail = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSDelivery>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(deliveriesEndpoints.reportDelivery, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        [deliveriesEndpoints.reportDelivery.meta.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { reportDetail: data };
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
const useReportsFacilities = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSFacility[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(deliveriesEndpoints.reportFacilities, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        [deliveriesEndpoints.reportFacilities.meta.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { reportFacilities: data };
};

export { useOrgDeliveries, useReportsDetail, useReportsFacilities };
