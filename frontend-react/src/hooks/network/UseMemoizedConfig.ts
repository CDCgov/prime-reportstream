import { useMemo } from "react";
import { Method } from "axios";

import { useSessionContext } from "../../contexts/SessionContext";
import {
    AdvancedConfig,
    API,
    createRequestConfig,
} from "../../network/api/NewApi";

/** Helper hook that converts `PrimeAdmins` to `ignore`
 * @todo Ticket to make PrimeAdmins an RS org {@link https://github.com/CDCgov/prime-reportstream/issues/4140 #4140}
 * @param orgName {string|undefined} Active membership `parsedName` */
export const useAdminSafeOrgName = (orgName: string | undefined) => {
    return useMemo(
        () => (orgName === "PrimeAdmins" ? "ignore" : orgName),
        [orgName]
    );
};

interface ConfigParams<P, D = any> {
    api: API;
    endpointKey: string;
    method: Method;
    parameters?: P;
    advancedConfig?: AdvancedConfig<D>;
}

/** Easier-to-read wrapper for stabilizing config params. The output of this is safe to
 * use with `useMemoizedConfig`
 *
 * @param config {ConfigParams<any>} Your config parameters
 * @param updateOn {any[]} Array of dependencies to update on (i.e. Reactive parameters) */
export const useMemoizedConfigParams = <P>(
    config: ConfigParams<P>,
    updateOn: any[]
) =>
    useMemo(
        () => ({
            ...config,
        }),
        [...updateOn] //eslint-disable-line
    );
/** Stabilizes the `RSRequestConfig | SimpleError` object returned from `createRequestConfig`
 *
 * @param configParams {ConfigParams<any>} The output of `useMemoizedConfigParams` */
export const useMemoizedConfig = ({
    api,
    endpointKey,
    method,
    parameters,
    advancedConfig,
}: ConfigParams<any>) => {
    const { memberships, oktaToken } = useSessionContext();
    const adminSafeOrgName = useAdminSafeOrgName(
        memberships.state.active?.parsedName
    );
    return useMemo(
        () =>
            createRequestConfig(
                api,
                endpointKey,
                method,
                oktaToken?.accessToken,
                adminSafeOrgName,
                parameters,
                advancedConfig
            ),
        [
            api,
            endpointKey,
            method,
            oktaToken?.accessToken,
            adminSafeOrgName,
            parameters,
            advancedConfig,
        ]
    );
};
