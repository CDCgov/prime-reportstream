import { useEffect, useMemo, useState } from "react";
import axios from "axios";

import { orgApi, Sender } from "../network/api/OrgApi";

const useSenderMode = (defaultOrg?: string, defaultSender?: string): string => {
    const [status, setStatus] = useState<string>("active");
    const endpoint = useMemo(() => {
        if (defaultOrg && defaultSender) {
            return orgApi.getSenderDetail(defaultOrg, defaultSender);
        }
    }, [defaultOrg, defaultSender]);

    useEffect(() => {
        /* Tests threw a "you can't update state after a ReactElement is
         * torn down" error, so this is measured for a conditional that
         * changes state in the .then() call. On teardown, this will
         * be set to false and the state will not attempt to update. */
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
