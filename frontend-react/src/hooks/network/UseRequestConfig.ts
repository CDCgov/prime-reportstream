import { useCallback, useEffect, useMemo, useState } from "react";
import axios, { AxiosPromise, Method } from "axios";

import { RSRequestConfig } from "../../network/api/NewApi";
import { SimpleError } from "../../utils/UsefulTypes";

export interface RequestHookResponse<D> {
    data: D | undefined;
    loading: boolean;
    error: string;
    trigger: () => void;
}

const DataSentRequest = ["POST", "PATCH", "PUT"];
export const needsData = (reqType: Method) =>
    DataSentRequest.includes(reqType.toUpperCase());
export const hasData = (req: RSRequestConfig): boolean =>
    req.data !== undefined;
export const deletesData = (reqType: Method) =>
    reqType === "delete" || reqType === "DELETE";
export const needsTrigger = (reqType: Method) =>
    reqType !== "GET" && reqType !== "get";
const typedAxiosCall = <D>(config: RSRequestConfig): AxiosPromise<D> => {
    switch (config.method) {
        case "POST":
        case "post":
            return axios.post<D>(config.url, config.data, config);
        case "PUT":
        case "put":
            return axios.put<D>(config.url, config.data, config);
        case "PATCH":
        case "patch":
            return axios.patch<D>(config.url, config.data, config);
        case "DELETE":
        case "delete":
            return axios.delete<D>(config.url, config);
        case "GET":
        case "get":
        default:
            return axios.get<D>(config.url, config);
    }
};

/*
 *
 * Generic is the type of data coming back from the server */
const useRequestConfig = <D>(
    config: RSRequestConfig | SimpleError
): RequestHookResponse<D> => {
    /* Boolean indicating if method needs to be triggered or not. */
    const onlyCallOnTrigger = useMemo(() => {
        if (config instanceof SimpleError) return true;
        return needsTrigger(config.method);
    }, [config]);
    /* Trigger to allow users to trigger a call (i.e. a POST, PUT, PATCH, or DELETE */
    const [triggerCall, setTriggerCall] = useState(0);
    /* Increments trigger to trigger axios call */
    const trigger = useCallback(() => {
        setTriggerCall(triggerCall + 1);
    }, [triggerCall]);
    const [data, setData] = useState<D | undefined>();
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>("");

    /* Fetches the data whenever the config passed in is changed.
     * To trigger a re-call, use the API controller provided from
     * useApi(). */
    useEffect(() => {
        /* This value is our way of confirming our effect is still running
         * That way, when we break down a component, no more state is will
         * try to be set. This helps with teardown mid network call. */
        let subscribed = true;
        setLoading(true);
        if (config instanceof SimpleError) {
            throw Error(`Your config threw an error: ${config.message}`);
        }
        const validDataSentThrough = () => {
            if (needsData(config.method) && !hasData(config) && subscribed) {
                setData(undefined);
                setLoading(false);
                throw Error("This call requires data to be passed in");
            }
        };
        const fetchAndStoreData = () => {
            if (onlyCallOnTrigger && triggerCall < 1) return;
            typedAxiosCall<D>(config)
                .then((res) => res.data)
                .then((data) => {
                    /* This is pretty opinionated on how WE handle deletes.
                     * It might benefit from a refactor later on. */
                    if (deletesData(config.method) && subscribed) {
                        setData(undefined);
                        setLoading(false);
                    } else if (subscribed) {
                        setData(data);
                        setLoading(false);
                    }
                })
                /* Verified that this catch call is necessary, the catch
                 * block below doesn't handle this Promise */
                .catch((e: any) => {
                    if (subscribed) setError(e.message);
                });
        };
        try {
            /* Pre-fetch validator(s). Could be useful to extend this
             * feature in the future. */
            validDataSentThrough();
            /* API fetch */
            fetchAndStoreData();
        } catch (e: any) {
            console.error(e.message);
            setData(undefined);
            setError(e.message);
            setLoading(false);
        }
        return () => {
            subscribed = false;
        };
    }, [config, onlyCallOnTrigger, triggerCall]);

    return {
        data,
        loading,
        error,
        trigger,
    };
};

export default useRequestConfig;
