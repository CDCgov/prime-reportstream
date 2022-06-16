import { Method } from "axios";
import { useMemo } from "react";

import {
    AdvancedConfig,
    API,
    createRequestConfig,
} from "../../network/api/NewApi";
import { useSessionContext } from "../../contexts/SessionContext";
import { Newable } from "../../utils/UsefulTypes";

import useRequestConfig, { RequestHookResponse } from "./UseRequestConfig";

interface EndpointHookResponse<D> extends Omit<RequestHookResponse<D>, "data"> {
    data: D | D[] | undefined;
}

/* Shallow compare function to ensure all keys are the same across incoming data
 * and our API resource */
export const passesObjCompare = (obj1: any, obj2: Newable<any>) => {
    const obj1Keys = Object.keys(obj1);
    const obj2Keys = Object.keys(new obj2(Object.values(obj1)));
    const clear1 = obj1Keys.every((key: string) => obj2Keys.includes(key));
    const clear2 = obj2Keys.every((key: string) => obj1Keys.includes(key));
    return clear1 && clear2;
};

const useEndpoint = <P, D>(
    api: API<D>,
    endpointKey: string,
    method: Method,
    parameters?: P,
    advancedConfig?: AdvancedConfig<D>
): EndpointHookResponse<D> => {
    const { oktaToken, memberships } = useSessionContext();
    const { data, loading, error, trigger } = useRequestConfig<D | D[]>(
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
    const typeCheckedData = useMemo(() => {
        if (data && data instanceof Array) {
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
        data: typeCheckedData,
        loading,
        error,
        trigger,
    };
};

export default useEndpoint;
