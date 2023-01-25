import { useCallback, useMemo } from "react";
import { AccessToken } from "@okta/okta-auth-js";

import { useAdminSafeOrganizationName } from "../../UseAdminSafeOrganizationName";
import { deliveriesEndpoints } from "../../../config/api/deliveries";
import { useSessionContext } from "../../../contexts/SessionContext";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../filters/UseFilterManager";
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

// TODO: Convert to useRSQuery
/** Hook consumes the ReportsApi "list" endpoint and delivers the response
 *
 * @param service {string} the chosen receiver service (e.x. `elr-secondary`)
 * */
export const useOrgDeliveries = (service?: string) => {
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
        [orgAndService, sortOrder, generateFetcher, rangeFrom, rangeTo]
    );

    return { fetchResults, filterManager };
};
