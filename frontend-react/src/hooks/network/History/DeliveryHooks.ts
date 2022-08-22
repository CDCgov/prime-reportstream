import { Method } from "axios";
import { useMemo } from "react";

import {
    DeliveryApi,
    DeliveryDetailParams,
    DeliveryListParams,
    RSDelivery,
    RSReportInterface,
} from "../../../network/api/History/Reports";
import {
    useAdminSafeOrgName,
    useMemoizedConfig,
    useMemoizedConfigParams,
} from "../UseMemoizedConfig";
import { BasicAPIResponse } from "../../../network/api/NewApi";
import useRequestConfig from "../UseRequestConfig";

/** Hook consumes the ReportsApi "list" endpoint and delivers the response
 *
 * @param org {string} the `parsedName` of user's active membership
 * @param service {string} the chosen receiver service (e.x. `elr-secondary`)
 * @returns {BasicAPIResponse<RSReportInterface[]>}
 * */
const useReportsList = (org?: string, service?: string) => {
    const adminSafeOrgName = useAdminSafeOrgName(org);
    const orgAndService = useMemo(
        () => `${adminSafeOrgName}.${service}`,
        [adminSafeOrgName, service]
    );
    const configParams = useMemoizedConfigParams<DeliveryListParams>(
        {
            api: DeliveryApi,
            endpointKey: "list",
            method: "GET" as Method,
            parameters: { orgAndService },
            advancedConfig: { requireTrigger: true },
        },
        [orgAndService]
    );
    const config = useMemoizedConfig(configParams);
    return useRequestConfig(config) as BasicAPIResponse<
        RSDelivery[] | RSReportInterface[]
    >;
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param id {string | number} Pass in the reportId OR deliveryId to query a single delivery
 * @returns {BasicAPIResponse<RSReportInterface>}
 * */
const useReportsDetail = (id: string | number) => {
    const memoizedId = useMemo(() => `${id}`, [id]); // Stringify & memoize
    const configParams = useMemoizedConfigParams<DeliveryDetailParams>(
        {
            api: DeliveryApi,
            endpointKey: "detail",
            method: "GET" as Method,
            parameters: { id: memoizedId },
        },
        [memoizedId]
    );
    const config = useMemoizedConfig(configParams);
    return useRequestConfig(config) as BasicAPIResponse<RSDelivery>;
};

export { useReportsList, useReportsDetail };
