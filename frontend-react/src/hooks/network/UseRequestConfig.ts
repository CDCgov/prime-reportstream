import { useEffect, useState } from "react";
import axios, { AxiosPromise, Method } from "axios";

import { RSRequestConfig } from "../../network/api/NewApi";

interface RequestHookResponse<D> {
    data: D | undefined;
    loading: boolean;
    error: string;
}

const DataSentRequest = ["POST", "PATCH", "PUT"];
export const needsData = (reqType: Method) =>
    DataSentRequest.includes(reqType.toUpperCase());
export const hasData = (req: RSRequestConfig): boolean =>
    req.data !== undefined;
export const deletesData = (reqType: Method) =>
    reqType === "delete" || reqType === "DELETE";
const typedAxiosCall = <T>(config: RSRequestConfig): AxiosPromise<T> => {
    switch (config.method) {
        case "POST":
        case "post":
            return axios.post<T>(config.url, config.data, config);
        case "PUT":
        case "put":
            return axios.put<T>(config.url, config.data, config);
        case "PATCH":
        case "patch":
            return axios.patch<T>(config.url, config.data, config);
        case "DELETE":
        case "delete":
            return axios.delete<T>(config.url, config);
        case "GET":
        case "get":
        default:
            return axios.get<T>(config.url, config);
    }
};

/*
 *
 * Generic is the type of data coming back from the server */
const useRequestConfig = <D>(
    config: RSRequestConfig
): RequestHookResponse<D> => {
    const [data, setData] = useState<D | undefined>();
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>("");

    /* Fetches the data whenever the config passed in is changed.
     * To trigger a re-call, use the API controller provided from
     * useApi(). */
    useEffect(() => {
        setLoading(true);
        const validDataSentThrough = () => {
            // TODO: Use API.resource for runtime type safety
            if (needsData(config.method) && !hasData(config)) {
                setData(undefined);
                setLoading(false);
                throw Error("This call requires data to be passed in");
            }
        };
        const fetchAndStoreData = () => {
            typedAxiosCall<D>(config)
                .then((res) => res.data)
                .then((data) => {
                    /* This is pretty opinionated on how WE handle deletes.
                     * It might benefit from a refactor later on. */
                    if (deletesData(config.method)) {
                        setData(undefined);
                        setLoading(false);
                    } else {
                        // TODO: Use API.resource to generate objects for runtime safety
                        setData(data);
                        setLoading(false);
                    }
                })
                /* Verified that this catch call is necessary, the catch
                 * block below doesn't handle this Promise */
                .catch((e: any) => {
                    setError(e.message);
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
    }, [config]);

    return {
        data,
        loading,
        error,
    };
};

export default useRequestConfig;
