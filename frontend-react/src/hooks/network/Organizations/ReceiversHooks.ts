import { Method } from "axios";

import {
    useAdminSafeOrgName,
    useMemoizedConfig,
    useMemoizedConfigParams,
} from "../UseMemoizedConfig";
import useRequestConfig from "../UseRequestConfig";
import { BasicAPIResponse } from "../../../network/api/NewApi";
import {
    ReceiverApi,
    ReceiverListParams,
    RSReceiver,
} from "../../../network/api/Organizations/Receivers";

/** Retrieves a list of Receivers for an org from the API
 * > **This call requires the use of `trigger()`**
 *
 * @param org {string?} The user's active membership `parsedName`
 * @deprecated Please use useOrganizationReceivers */
export const useReceiversList = (org?: string) => {
    const memoizedSafeOrg = useAdminSafeOrgName(org);
    const configParams = useMemoizedConfigParams<ReceiverListParams>(
        {
            api: ReceiverApi,
            endpointKey: "list",
            method: "GET" as Method,
            parameters: { org: memoizedSafeOrg || "" },
            advancedConfig: { requireTrigger: true },
        },
        [memoizedSafeOrg]
    );
    const config = useMemoizedConfig(configParams);
    return useRequestConfig(config) as BasicAPIResponse<RSReceiver[]>;
};
