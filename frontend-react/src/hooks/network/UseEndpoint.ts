import { Method } from "axios";
import { useEffect, useMemo, useState } from "react";

import {
    AdvancedConfig,
    API,
    createRequestConfig,
} from "../../network/api/NewApi";
import { useSessionContext } from "../../contexts/SessionContext";
import { Newable } from "../../utils/UsefulTypes";

import useRequestConfig, { RequestHookResponse } from "./UseRequestConfig";

interface EndpointHookResponse<D> extends RequestHookResponse<D> {
    loading: boolean;
}

/* TODO: Write a great object compare function */
/* Shallow compare function to ensure all keys are the same across incoming data
 * and our API resource */
export const passesObjCompare = (obj1: any, obj2: Newable<any>) => {
    const obj1Keys = Object.keys(obj1);
    const obj2Keys = Object.keys(new obj2(Object.values(obj1)));
    const clear1 = obj1Keys.every((key: string) => obj2Keys.includes(key));
    const clear2 = obj2Keys.every((key: string) => obj1Keys.includes(key));
    return clear1 && clear2;
};

/* Generics provided for data type (D) and params (P). Good examples in test file. */
const useEndpoint = <D = any, P = {}>(
    api: API<D>,
    endpointKey: string,
    method: Method,
    parameters?: P,
    advancedConfig?: AdvancedConfig<D>
): EndpointHookResponse<D> => {
    const { oktaToken, memberships } = useSessionContext();
    const { data, error, trigger } = useRequestConfig<D>(
        createRequestConfig(
            api,
            endpointKey,
            method,
            oktaToken?.accessToken,
            memberships.state.active?.parsedName,
            parameters,
            advancedConfig
        )
    );
    /* Maintains loading state by looking for whether data or and error are
     * passed back by the useRequestConfig hook */
    const [loading, setLoading] = useState<boolean>(true);
    useEffect(() => {
        if (data !== undefined || error !== "") {
            setLoading(false);
        } else {
            setLoading(true);
        }
    }, [data, error]);

    const dataAsResource = useMemo(() => {
        if (data && data instanceof Array) {
            const valid = data.every((obj) =>
                passesObjCompare(obj, api.resource)
            );
            if (!valid) return undefined;
            return data.map((item: D) => {
                const args = Object.values(item);
                return new api.resource(...args);
            });
        } else if (data && passesObjCompare(data, api.resource)) {
            const args = Object.values(data);
            return new api.resource(...args);
        }
    }, [api.resource, data]);

    return {
        data: dataAsResource,
        loading,
        error,
        trigger,
    };
};

export default useEndpoint;
