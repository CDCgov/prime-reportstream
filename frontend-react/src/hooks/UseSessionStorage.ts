import { useEffect, useMemo, useState } from "react";

import {
    setStoredOrg,
    setStoredSenderName,
} from "../contexts/SessionStorageTools";

interface SessionStore {
    org: string;
    senderName: string;
}

export interface SessionController {
    values: SessionStore;
    updateSessionStorage: (p: Partial<SessionStore>) => void;
}

const useSessionStorage = (): SessionController => {
    const [org, setOrg] = useState<string>("testOrg");
    const [senderName, setSenderName] = useState<string>("testSender");
    const state = useMemo(() => {
        return {
            org: org,
            senderName: senderName,
        };
    }, [org, senderName]);

    const updateSessionStorage = (values: Partial<SessionStore>) => {
        if (values.org) setOrg(values.org);
        if (values.senderName) setSenderName(values.senderName);
    };

    useEffect(() => {
        setStoredOrg(org);
    }, [org]);

    useEffect(() => {
        setStoredSenderName(senderName);
    }, [senderName]);

    return {
        values: state,
        updateSessionStorage: updateSessionStorage,
    };
};

export default useSessionStorage;
