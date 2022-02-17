import { useState, useEffect } from "react";
import { Endpoint } from "../NetworkTypes";

interface ResponseType<T> {
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
        api.instance
            .get<T>(url)
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
    }, [api.instance, url]);

    return response;
}
