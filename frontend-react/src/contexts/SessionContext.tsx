import { createContext, FC, useContext } from "react";

import useSessionStorage, { StoreController } from "../hooks/UseSessionStorage";
import { MembershipController, useGroups } from "../hooks/UseGroups";

interface ISessionContext {
    memberships: MembershipController;
    store: StoreController;
}

export const SessionContext = createContext<ISessionContext>({
    memberships: {} as MembershipController,
    store: {} as StoreController,
});

const SessionProvider: FC = ({ children }) => {
    const store = useSessionStorage();
    const memberships = useGroups();

    return (
        <SessionContext.Provider
            value={{
                memberships: memberships,
                store: store,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
