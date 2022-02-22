import { useState, useEffect } from "react";
import axios from "axios";

import { Endpoint, Api } from "../api/Api";
import { useCacheContext } from "../cache/NetworkCache";
import { useCache } from "./useCache";

export interface ResponseType<T> {
    loading: boolean;
    data?: T;
    status: number;
    message: string;
}

export function useNetwork<T>({ url, api }: Endpoint): ResponseType<T> {
    const [response, setResponse] = useState<ResponseType<T>>({
        loading: true,
        data: undefined,
        status: 0,
        message: "",
    });
    const { updateCache } = useCacheContext();
    const cachedResult = useCache({ url, api });

    useEffect(() => {
        if (!cachedResult || !cachedResult?.response?.data) {
            /* Fetch data and handle any parsing needed */
            axios
                .get<T>(url, api.config)
                .then((res) => {
                    const networkResponse = {
                        loading: false,
                        data: res.data,
                        status: res.status,
                        message: "",
                    };
                    setResponse(networkResponse);

                    /* BUG: This is calling the updateCache() function from my empty
                declaration and NOT from the Cache state item given by the provider */
                    debugger;
                    updateCache({
                        endpoint: { url: url, api: api } as Endpoint,
                        response: networkResponse,
                    });
                })
                .catch((err) => {
                    setResponse({
                        loading: false,
                        data: undefined,
                        status: err.response.status,
                        message: err.message,
                    });
                });
        } else {
            setResponse(cachedResult.response);
        }
    }, [api.config, url, cachedResult]);

    return response;
}

export function callApi<T>(url: string, api: typeof Api) {
    return axios.get<T>(url, api.config);
}
