import { useCallback, useEffect, useState } from "react";
import axios, { Method } from "axios";

import { RSRequestConfig } from "../../network/api/NewApi";
import { SimpleError } from "../../utils/UsefulTypes";

export interface RequestHookResponse<D> {
    data: D | D[] | undefined;
    error: string;
    loading: boolean;
    trigger: () => void;
}

const DataSentRequest = ["POST", "PATCH", "PUT"];

/* A bunch of useful boolean checks to keep logic clean below */
export const needsData = (reqType: Method) =>
    DataSentRequest.includes(reqType.toUpperCase());
export const hasData = (req: RSRequestConfig): boolean =>
    req.data !== undefined;
export const deletesData = (reqType: Method) =>
    reqType.toUpperCase() === "DELETE";
export const needsTrigger = (reqType: Method) =>
    reqType.toUpperCase() !== "GET";
export const urlMissingVariables = (url: string) => {
    const urlObj = new URL(url);
    return urlObj.pathname.includes(":");
};

/* Takes the output from `createRequestConfig` and uses it to
 * make an Axios call. The reason we use an AxiosConfig (extended
 * by RSRequestConfig) is to achieve the same functionality as
 * calling `axios(config)` but with compile-time type safety for the
 * function. As of writing this, only specific axios functions like
 * `axios.get` and `axios.post` have generics we can use, so we have
 * to use `typedAxiosCall` as our proxy for selecting a method and use
 * the generic to set a data type.
 *
 * Generic is the type of data coming back from the server */
const useRequestConfig = <D>(
    config: RSRequestConfig | SimpleError
): RequestHookResponse<D> => {
    /* Boolean indicating if method needs to be triggered or not. */
    const onlyCallOnTrigger = useCallback((givenConfig: RSRequestConfig) => {
        return needsTrigger(givenConfig.method);
    }, []);
    /* Trigger to allow users to trigger a call (i.e. a POST, PUT, PATCH, or DELETE */
    const [triggerCall, setTriggerCall] = useState(0);
    /* Increments trigger to trigger axios call */
    const trigger = useCallback(() => {
        setTriggerCall(triggerCall + 1);
        // Temp solution for loading state on trigger
        setLoading(true);
    }, [triggerCall]);
    const [data, setData] = useState<D | undefined>();
    const [error, setError] = useState<string>("");
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

    /* Fetches the data whenever the config passed in is changed.
     * To trigger a re-call, use the API controller provided from
     * useApi(). */
    useEffect(() => {
        /* This value is our way of confirming our effect is still running
         * That way, when we break down a component, no more state is will
         * try to be set. This helps with teardown mid network call. */
        let subscribed = true;
        try {
            if (config instanceof SimpleError) {
                throw Error(`Your config threw an error: ${config.message}`);
            }
            if (needsData(config.method) && !hasData(config) && subscribed) {
                throw Error("This call requires data to be passed in");
            }
            /* API fetch */
            if (onlyCallOnTrigger(config) && triggerCall < 1) {
                setLoading(false);
                return;
            }

            if (!urlMissingVariables(config.url)) {
                axios(config)
                    .then((res) => {
                        /* This is pretty opinionated on how WE handle deletes.
                         * It might benefit from a refactor later on. */
                        if (deletesData(config.method) && subscribed) {
                            setData({} as D);
                        } else if (subscribed) {
                            setData(res.data);
                        }
                    })
                    /* Verified that this catch call is necessary, the catch
                     * block below doesn't handle this Promise */
                    .catch((e: any) => {
                        if (subscribed) {
                            setError(e.message);
                        }
                    });
            }
        } catch (e: any) {
            setLoading(false);
            setData(undefined);
            setError(e.message);
            console.error(e.message);
        }
        return () => {
            subscribed = false;
        };
    }, [config, onlyCallOnTrigger, triggerCall]);

    return {
        data,
        error,
        loading,
        trigger,
    };
};

export default useRequestConfig;
