import { createContext, FC } from "react";

import useSessionStorage, {
    SessionController
} from "../hooks/UseSessionStorage";
import {MembershipController, useGroups} from "../hooks/UseGroups";

interface ISessionContext {
    memberships: MembershipController
    store: SessionController
}

export const SessionContext = createContext<ISessionContext>({
    memberships: {} as MembershipController,
    store: {} as SessionController
});

const SessionProvider: FC = ({ children }) => {
    const store = useSessionStorage();
    const memberships = useGroups()

    return (
        <SessionContext.Provider value={{
            memberships: memberships,
            store: store
        }}>
            {children}
        </SessionContext.Provider>
    );
};

export default SessionProvider;
