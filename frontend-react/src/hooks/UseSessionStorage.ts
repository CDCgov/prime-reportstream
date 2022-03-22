import { useEffect, useState } from "react";

import {
    getStoredOrg,
    getStoredSenderName,
    setStoredOrg,
    setStoredSenderName,
} from "../contexts/SessionStorageTools";

export interface SessionStore {
    org?: string;
    senderName?: string;
}

export interface SessionController {
    values: SessionStore;
    updateSessionStorage: (p: Partial<SessionStore>) => void;
}

/* TODO: this isn't reactively updating  */
const useSessionStorage = (): SessionController => {
    // const [org, setOrg] = useState<string | undefined>(getStoredOrg());
    // const [senderName, setSenderName] = useState<string | undefined>(getStoredSenderName());
    const [values, setValues] = useState<SessionStore>({
        org: getStoredOrg(),
        senderName: getStoredSenderName(),
    });

    const updateSessionStorage = (values: Partial<SessionStore>) => {
        debugger;
        setValues({
            org: values.org,
            senderName: values.senderName,
        });
    };

    useEffect(() => {
        if (values.org) {
            setStoredOrg(values.org);
        }
    }, [values.org]);

    useEffect(() => {
        if (values.senderName) {
            setStoredSenderName(values.senderName);
        }
    }, [values.senderName]);

    return {
        values: values,
        updateSessionStorage: updateSessionStorage,
    };
};

export default useSessionStorage;
