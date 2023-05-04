import { AccessToken } from "@okta/okta-auth-js";
import { useCallback, useMemo } from "react";

import { useSessionContext } from "../../../contexts/SessionContext";
import { useCreateFetch } from "../../UseCreateFetch";
import { MembershipSettings } from "../../UseOktaMemberships";
import {
    Organizations,
    useAdminSafeOrganizationName,
} from "../../UseAdminSafeOrganizationName";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../filters/UseFilterManager";
import {
    dataDashboardEndpoints,
    RSDelivery,
} from "../../../config/endpoints/dataDashboard";

const { getOrgDeliveries } = dataDashboardEndpoints;

export enum DataDashboardAttr {
    REPORT_ID = "reportId",
    DATE_SENT = "batchReadyAt",
    PROVIDER = "orderingProvider",
    FACILITY = "performingFacility",
    SUBMITTER = "submitter",
}

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: DataDashboardAttr.DATE_SENT,
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

export { useOrgDeliveries };
