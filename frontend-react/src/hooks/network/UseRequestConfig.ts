import { useEffect, useState } from "react";
import axios, { AxiosPromise, Method } from "axios";

import { RSRequestConfig } from "../../network/api/NewApi";

interface RequestHookResponse<D> {
    data: D | undefined;
    loading: boolean;
    error: string;
    //TODO: Add error type in here. Axios errors?
}

const DataSentRequest = ["POST", "PATCH", "PUT"];
const needsData = (reqType: Method) =>
    DataSentRequest.includes(reqType.toUpperCase());
const hasData = (req: RSRequestConfig): boolean => req.data !== undefined;
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
    const [error, setError] = useState(""); //TODO: Type error

    /* Fetches the data whenever the config passed in is changed.
     * To trigger a re-call, use the API controller provided from
     * useApi(). */
    useEffect(() => {
        setLoading(true);
        try {
            if (needsData(config.method) && !hasData(config)) {
                setData(undefined);
                setLoading(false);
                throw Error("This call requires data to be passed in");
            }
            typedAxiosCall<D>(config)
                .then((res) => res.data)
                .then((data) => {
                    // TODO: Use data at useApi level to generate objects for runtime safety
                    setData(data);
                    setLoading(false);
                })
                .catch((e: any) => {
                    setError(e.message);
                });
            // Still need .catch()?
        } catch (e: any) {
            console.error(e.message);
            setError(e.message);
        }
    }, [config]);

    return {
        data,
        loading,
        error,
    };
};

export default useRequestConfig;
