import { useCallback, useEffect, useMemo, useState } from "react";
import axios from "axios";

import { orgApi, Sender } from "../network/api/OrgApi";
import {
    getStoredOrg,
    getStoredSenderName,
} from "../contexts/SessionStorageTools";

export interface SenderStatus {
    status: string;
    update: (a: string, b: string) => void;
}

const useSenderMode = (
    defaultOrg?: string,
    defaultSender?: string
): SenderStatus => {
    const [status, setStatus] = useState<string>("active");
    const [org, setOrg] = useState<string>(defaultOrg || getStoredOrg());
    const [sender, setSender] = useState<string>(
        defaultSender || getStoredSenderName()
    );
    const endpoint = useMemo(() => {
        return orgApi.getSenderDetail(org, sender);
    }, [org, sender]);

    /* FOR TESTING ONLY:
     * Because of how react-hooks-testing-library renders and acts on hooks, the hook
     * needs to both have a default behavior (found in useEffect), and let me act on it
     * to trigger the call in a test suite to ensure values return as intended. */
    const update = (newOrg: string, newSender?: string) => {
        setOrg(newOrg);
        setSender(newSender || "");
    };

    useEffect(() => {
        if (org !== "" && sender !== "") {
            axios
                .get<Sender>(endpoint.url, endpoint)
                .then((res) => setStatus(res.data.customerStatus));
        }
    }, [endpoint, org, sender]);

    return {
        status: status,
        update: update,
    };
};

export default useSenderMode;
