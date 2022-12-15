import { useCallback, useMemo } from "react";
import { AccessToken } from "@okta/okta-auth-js";

import { useAdminSafeOrganizationName } from "../../UseAdminSafeOrganizationName";
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
import { useCreateFetch } from "../../UseCreateFetch";
import { MembershipSettings } from "../../UseOktaMemberships";

const { getOrgDeliveries, getDeliveryDetails, getDeliveryFacilities } =
    deliveriesEndpoints;

export enum DeliveriesDataAttr {
    REPORT_ID = "reportId",
    BATCH_READY = "batchReadyAt",
    EXPIRES = "expires",
    ITEM_COUNT = "reportItemCount",
    FILE_TYPE = "fileType",
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
            const fetcher = generateFetcher();
            return fetcher(getOrgDeliveries, {
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
        [orgAndService, sortOrder, generateFetcher, rangeFrom, rangeTo]
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

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string} Pass in the reportId to query for facilities on a report
 * */
const useReportsFacilities = (id: string) => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSFacility[]>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getDeliveryFacilities, {
                segments: {
                    id: id,
                },
            }),
        [authorizedFetch, id]
    );
    const { data } = rsUseQuery(
        // sets key with orgAndService so multiple queries can be cached when viewing multiple detail pages
        // during use
        [getDeliveryFacilities.queryKey, id],
        memoizedDataFetch,
        { enabled: !!id }
    );
    return { reportFacilities: data };
};

export { useOrgDeliveries, useReportsDetail, useReportsFacilities };
