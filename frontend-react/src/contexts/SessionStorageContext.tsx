import { createContext, FC } from "react";

import useSessionStorage, {
    SessionController,
    SessionStore,
} from "../hooks/UseSessionStorage";

export const SessionStorageContext = createContext<SessionController>({
    values: {},
    updateSessionStorage: (store: Partial<SessionStore>) => {
        console.log("to please SonarCloud");
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
