import { useEffect, useMemo, useState } from "react";
import axios from "axios";

import { orgApi, Sender } from "../network/api/OrgApi";

const useSenderMode = (defaultOrg?: string, defaultSender?: string): string => {
    const [status, setStatus] = useState<string>("active");
    const endpoint = useMemo(() => {
        debugger;
        if (defaultOrg && defaultSender) {
            return orgApi.getSenderDetail(defaultOrg, defaultSender);
        }
    }, [defaultOrg, defaultSender]);

    useEffect(() => {
        let isSubscribed = true;
        if (endpoint) {
            axios
                .get<Sender>(endpoint.url, endpoint)
                .then((res) =>
                    isSubscribed ? setStatus(res.data.customerStatus) : null
                );
        }
        return () => {
            isSubscribed = false;
        };
    }, [endpoint]);

    return status;
};

export default useSenderMode;
