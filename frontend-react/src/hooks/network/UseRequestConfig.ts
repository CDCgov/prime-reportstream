import { useEffect, useState } from "react";
import axios from "axios";

import { RSRequestConfig } from "../../network/api/NewApi";

interface RequestHookResponse<D> {
    data: D;
    loading: boolean;
}

/*
 *
 * Generic is the type of data coming back from the server */
const useRequestConfig = <D>(
    config: RSRequestConfig
): RequestHookResponse<D> => {
    const [data, setData] = useState<D>({} as D);
    const [loading, setLoading] = useState<boolean>(true);

    useEffect(() => {
        setLoading(true);
        axios(config)
            .then((res) => res.data)
            .then((data) => {
                setData(data as D);
                setLoading(false);
            });
    }, [config]);

    return {
        data,
        loading,
    };
};

export default useRequestConfig;
