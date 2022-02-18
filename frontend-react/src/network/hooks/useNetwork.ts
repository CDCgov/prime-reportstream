import { useState, useEffect } from "react";
import axios, { AxiosRequestConfig } from 'axios'

import { Endpoint } from "../NetworkTypes";
import { Api } from "../api/Api";

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

    const callNetwork = async (url: string, config: AxiosRequestConfig<T>) => {
        return await axios.get<T>(url, config)
            .then((res) => {
                setResponse({
                    loading: false,
                    data: res.data,
                    status: res.status,
                    message: "",
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
    }

    useEffect(() => {
        /* Fetch data and handle any parsing needed */
        callNetwork(url, api.config)

    }, [api.config, url]);

    return response;
}

export function callApi<T>(url: string, api: typeof Api) {
    return axios.get<T>(url, api.config)
}
