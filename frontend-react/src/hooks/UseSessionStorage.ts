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

const useSessionStorage = (): SessionController => {
    const [values, setValues] = useState<SessionStore>({
        org: getStoredOrg(),
        senderName: getStoredSenderName(),
    });

    const updateSessionStorage = (newValues: Partial<SessionStore>) => {
        setValues({
            org: newValues.org,
            senderName: newValues.senderName,
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
