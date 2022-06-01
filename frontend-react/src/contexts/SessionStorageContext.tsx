import { createContext, FC, useContext /* useReducer */ } from "react";

import useSessionStorage, {
    SessionController,
} from "../hooks/UseSessionStorage";
import { MembershipController, useGroups } from "../hooks/UseGroups";

interface ISessionContext {
    memberships: MembershipController;
    store: SessionController;
}

export const SessionContext = createContext<ISessionContext>({
    memberships: {} as MembershipController,
    store: {} as SessionController,
});

const SessionProvider: FC = ({ children }) => {
    const store = useSessionStorage();
    const memberships = useGroups();
    // const [fakeMemberState, fakeMemberDispatch] = useReducer(() => ({}), {});

    return (
        <SessionContext.Provider
            value={{
                // memberships: {
                //     state: fakeMemberState,
                //     dispatch: fakeMemberDispatch,
                // },
                memberships,
                store: store,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
