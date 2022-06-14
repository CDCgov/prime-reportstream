import { useEffect, useState } from "react";
import axios, { AxiosPromise } from "axios";

import { RSRequestConfig } from "../../network/api/NewApi";

interface RequestHookResponse<D> {
    data: D | undefined;
    loading: boolean;
}

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

    /* Fetches the data whenever the config passed in is changed.
     * To trigger a re-call, use the API controller provided from
     * useApi(). */
    useEffect(() => {
        setLoading(true);
        typedAxiosCall<D>(config)
            .then((res) => res.data)
            .then((data) => {
                setData(data);
                setLoading(false);
            });
    }, [config]);

    return {
        data,
        loading,
    };
};

export default useRequestConfig;
