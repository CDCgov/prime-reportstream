import { useCallback, useMemo, useState } from "react";
import { deliveriesEndpoints, RSDelivery } from "../../../../config/endpoints/deliveries";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import useFilterManager, { FilterManagerDefaults } from "../../../filters/UseFilterManager/UseFilterManager";
import useAdminSafeOrganizationName, {
    Organizations,
} from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { getOrgDeliveries } = deliveriesEndpoints;

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

export type SearchFetcher<T> = (additionalParams?: SearchParams) => Promise<T[]>;

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
    const [service, setService] = useState(initialService ?? "");
    const { activeMembership, authorizedFetch } = useSessionContext();

    const adminSafeOrgName = useAdminSafeOrganizationName(activeMembership?.parsedName); // "PrimeAdmins" -> "ignore"
    const orgAndService = useMemo(
        () => (service ? `${adminSafeOrgName}.${service}` : `${adminSafeOrgName}`),
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

            return authorizedFetch<RSDelivery[]>(
                {
                    segments: {
                        orgAndService: orgAndService,
                    },
                    params,
                },
                getOrgDeliveries,
            );
        },
        [activeMembership?.parsedName, sortOrder, rangeFrom, rangeTo, orgAndService, authorizedFetch],
    );

    return { fetchResults, filterManager, setService };
};

export default useOrgDeliveries;
