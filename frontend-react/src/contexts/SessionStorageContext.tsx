import { createContext, useContext } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

import useSessionStorage, {
    SessionController,
} from "../hooks/UseSessionStorage";
import { MembershipController, useGroups } from "../hooks/UseGroups";

interface ISessionContext {
    memberships: MembershipController;
    store: SessionController;
}

export type BasicOktaHook = () => {
    authState: {
        accessToken: AccessToken;
    };
    oktaAuth: {};
};

interface ISessionProviderProps {
    oktaHook: BasicOktaHook | (() => IOktaContext);
}

export const SessionContext = createContext<ISessionContext>({
    memberships: {} as MembershipController,
    store: {} as SessionController,
});

const SessionProvider = ({
    children,
    oktaHook,
}: React.PropsWithChildren<ISessionProviderProps>) => {
    const { authState } = oktaHook();
    const store = useSessionStorage();
    const memberships = useGroups(authState?.accessToken);

    return (
        <SessionContext.Provider
            value={{
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
