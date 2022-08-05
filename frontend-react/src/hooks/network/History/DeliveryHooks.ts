import { Method } from "axios";
import { useMemo } from "react";

import {
    DeliveryApi,
    DeliveryDetailParams,
    DeliveryListParams,
    RSDelivery,
} from "../../../network/api/History/Reports";
import { useMemoizedConfig, useMemoizedConfigParams } from "../UseApiEndpoint";
import { BasicAPIResponse } from "../../../network/api/NewApi";
import useRequestConfig from "../UseRequestConfig";

/** Hook consumes the ReportsApi "list" endpoint and delivers the response
 *
 * @param org {string} the `parsedName` of user's active membership
 * @param service {string} the chosen receiver service (e.x. `elr-secondary`)
 * @returns {BasicAPIResponse<RSReportInterface[]>}
 * */
const useReportsList = (org?: string, service?: string) => {
    const orgAndService = useMemo(() => `${org}.${service}`, [org, service]);
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
    return useRequestConfig(config) as BasicAPIResponse<RSDelivery[]>;
};

/** Hook consumes the ReportsApi "detail" endpoint and delivers the response
 *
 * @param deliveryId {number} Pass in the reportId to query the API
 * @returns {BasicAPIResponse<RSReportInterface>}
 * */
const useReportsDetail = (deliveryId: number) => {
    const memoizedId = useMemo(() => deliveryId, [deliveryId]);
    const configParams = useMemoizedConfigParams<DeliveryDetailParams>(
        {
            api: DeliveryApi,
            endpointKey: "detail",
            method: "GET" as Method,
            parameters: { deliveryId: memoizedId },
            advancedConfig: { requireTrigger: true },
        },
        [memoizedId]
    );
    const config = useMemoizedConfig(configParams);
    return useRequestConfig(config) as BasicAPIResponse<RSDelivery>;
};

export { useReportsList, useReportsDetail };
