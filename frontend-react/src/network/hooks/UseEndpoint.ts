import { useState, useCallback } from "react";
import axios, { AxiosResponse } from "axios";

import { EndpointConfig } from "../api/Api";

export interface EndpointResponse<T> {
    loading: boolean;
    data?: T;
    status: number;
    message: string;
}
export interface EndpointController<T> {
    call: () => void;
    response: EndpointResponse<T>;
}

export function useEndpoint<T>(
    endpoint: EndpointConfig<T>
): EndpointController<T> {
    const [response, setResponse] = useState<EndpointResponse<T>>({
        loading: true,
        data: undefined,
        status: 0,
        message: "",
    });

    const handleResponse = (res: AxiosResponse) => {
        setResponse({
            loading: false,
            data: res.data || null,
            status: res.status,
            message: "",
        });
    };

    const handleError = (err: any) => {
        setResponse({
            loading: false,
            data: undefined,
            status: err.response?.status || 400,
            message: err.message,
        });
    };

    const call = useCallback(() => {
        switch (endpoint.method) {
            case "GET": {
                axios
                    .get<T>(endpoint.url, endpoint)
                    .then((res: AxiosResponse) => {
                        handleResponse(res);
                    })
                    .catch((err: any) => {
                        handleError(err);
                    });
                break;
            }
            case "POST": {
                axios
                    .post<T>(endpoint.url, endpoint)
                    .then((res: AxiosResponse) => {
                        handleResponse(res);
                    })
                    .catch((err: any) => {
                        handleError(err);
                    });
                break;
            }
            case "PATCH": {
                axios
                    .patch<T>(endpoint.url, endpoint)
                    .then((res: AxiosResponse) => {
                        handleResponse(res);
                    })
                    .catch((err: any) => {
                        handleError(err);
                    });
                break;
            }
            case "DELETE": {
                axios
                    .delete<T>(endpoint.url, endpoint)
                    .then((res: AxiosResponse) => {
                        handleResponse(res);
                    })
                    .catch((err: any) => {
                        handleError(err);
                    });
                break;
            }
        }
    }, [endpoint]);

    return { call, response };
}
