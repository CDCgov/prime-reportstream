import { createContext, FC, useMemo } from "react";

import useSessionStorage, { SessionStore } from "../hooks/UseSessionStorage";

interface ISessionContext {
    values: SessionStore;
    updateSessionStorage: (store: Partial<SessionStore>) => void;
}

export const SessionStorageContext = createContext<ISessionContext>({
    values: {},
    updateSessionStorage: (store: Partial<SessionStore>) => {
        console.log("to please SonarCloud");
    },
});

const SessionProvider: FC = ({ children }) => {
    const { values, updateSessionStorage } = useSessionStorage();

    const payload = useMemo(() => {
        return {
            values: values,
            updateSessionStorage: updateSessionStorage,
        };
    }, [values, updateSessionStorage]);

    return (
        <SessionStorageContext.Provider value={payload}>
            {children}
        </SessionStorageContext.Provider>
    );
};

export default SessionProvider;
