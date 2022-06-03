import { createContext, useContext } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import useSessionStorage, {
    SessionController,
} from "../hooks/UseSessionStorage";
import { MembershipController, useGroups } from "../hooks/UseGroups";

interface ISessionContext {
    memberships: MembershipController;
    store: SessionController;
}

export type OktaHook = () => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<ISessionContext>({
    memberships: {} as MembershipController,
    store: {} as SessionController,
});

// accepts `oktaHook` as a paremeter in order to allow mocking of this provider's okta based
// behavior for testing. In non test cases this hook will be the `useOktaAuth` hook from
// `okta-react`
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
