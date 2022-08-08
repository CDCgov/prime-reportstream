import { useMemo } from "react";
import { Method } from "axios";

import {
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
 * @param org {string?} The user's active membership `parsedName` */
export const useReceiversList = (org?: string) => {
    const memoizedOrg = useMemo(() => org, [org]);
    const configParams = useMemoizedConfigParams<ReceiverListParams>(
        {
            api: ReceiverApi,
            endpointKey: "list",
            method: "GET" as Method,
            parameters: { org: memoizedOrg || "" },
            advancedConfig: { requireTrigger: true },
        },
        [memoizedOrg]
    );
    const config = useMemoizedConfig(configParams);
    return useRequestConfig(config) as BasicAPIResponse<RSReceiver[]>;
};
