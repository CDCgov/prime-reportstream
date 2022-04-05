import { createContext, FC } from "react";

import useSessionStorage, {
    SessionController,
    SessionStore,
} from "../hooks/UseSessionStorage";

export const SessionStorageContext = createContext<SessionController>({
    values: {},
    updateSessionStorage: (store: Partial<SessionStore>) => {
        // Never gets called, just used store to please the linter
        console.log(`${store}`);
    },
});

const SessionProvider: FC = ({ children }) => {
    const payload = useSessionStorage();

    return (
        <SessionStorageContext.Provider value={payload}>
            {children}
        </SessionStorageContext.Provider>
    );
};

export default SessionProvider;
