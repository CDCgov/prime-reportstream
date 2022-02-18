import { useState, useEffect } from "react";
import axios from "axios";

import { Endpoint, Api } from "../api/Api";

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

    useEffect(() => {
        /* Fetch data and handle any parsing needed */
        axios
            .get<T>(url, api.config)
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
    }, [api.config, url]);

    return response;
}

export function callApi<T>(url: string, api: typeof Api) {
    return axios.get<T>(url, api.config);
}
