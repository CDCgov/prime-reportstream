import { useMemo } from "react";
import { Method } from "axios";

import { useSessionContext } from "../../contexts/SessionContext";
import {
    AdvancedConfig,
    API,
    BasicAPIResponse,
    createRequestConfig,
} from "../../network/api/NewApi";
import { StringIndexed } from "../../utils/UsefulTypes";

import useRequestConfig from "./UseRequestConfig";

/** Helper hook that converts `PrimeAdmins` to `ignore`
 * @todo Ticket to make PrimeAdmins an RS org {@link https://github.com/CDCgov/prime-reportstream/issues/4140 #4140}
 * @param orgName {string|undefined} Active membership `parsedName` */
const useAdminSafeOrgName = (orgName: string | undefined) => {
    return useMemo(
        () => (orgName === "PrimeAdmins" ? "ignore" : orgName),
        [orgName]
    );
};

/** Takes `API.endpoints` info and loads up the request config. GET will fetch on load,
 * all others can be triggered with `trigger()` through interface interactions or effect
 * hooks.
 *
 * @param api {API} An API object containing one or more endpoint
 * @param endpointKey {string} The key of the endpoint in API.endpoints
 * @param method {Method} any Axios network method (e.x. GET, POST)
 * @param parameters {<P>} Parameter shape. Define these in your API file
 * @param advancedConfig {AdvancedConfig<D>} Extend functionality with all other Axios features! */
export const useApiEndpoint = <P extends StringIndexed, D = any>(
    api: API,
    endpointKey: string,
    method: Method,
    parameters?: P,
    // Allows us to use more of AxiosRequestConfig if we want
    advancedConfig?: AdvancedConfig<D>
) => {
    const { memberships, oktaToken } = useSessionContext();
    const adminSafeOrgName = useAdminSafeOrgName(
        memberships.state.active?.parsedName
    );
    const config = useMemo(
        () =>
            createRequestConfig<P, D>(
                api,
                endpointKey,
                method,
                oktaToken?.accessToken,
                adminSafeOrgName,
                parameters,
                advancedConfig
            ),
        /* This, for some reason, cannot handle reacting to anything passed in
         * as a param. It infinitely updates `config`, thus infinitely calling the
         * `useRequestConfig` call below. This doesn't present bugs _now_, but if you
         * are the unlucky soul who found this message looking for hope...
         *
         * ...I am sorry */
        [oktaToken?.accessToken, adminSafeOrgName] //eslint-disable-line
    );
    // TODO: maybe reactive problems stem from this
    const { data, error, loading, trigger } = useRequestConfig(
        config
    ) as BasicAPIResponse<D>;

    return {
        data,
        error,
        loading,
        trigger,
    };
};
