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

export interface StoreController {
    values: SessionStore;
    updateSessionStorage: (p: Partial<SessionStore>) => void;
}

const isNotEmpty = (s: string): boolean => s.length > 0;

const useSessionStorage = (): StoreController => {
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
        if (values.org && isNotEmpty(values.org)) {
            setStoredOrg(values.org);
        }
    }, [values.org]);

    useEffect(() => {
        if (values.senderName && isNotEmpty(values.senderName)) {
            setStoredSenderName(values.senderName);
        }
    }, [values.senderName]);

    return {
        values: values,
        updateSessionStorage: updateSessionStorage,
    };
};

export default useSessionStorage;
