import React, { createContext, useContext } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

import useSessionStorage, {
    SessionController,
} from "../hooks/UseSessionStorage";
import {
    MembershipController,
    useOktaMemberships,
} from "../hooks/UseOktaMemberships";

export interface ISessionContext {
    memberships: MembershipController;
    store: SessionController;
    oktaToken?: Partial<AccessToken>;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<ISessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    memberships: {} as MembershipController,
    store: {} as SessionController,
});

// accepts `oktaHook` as a parameter in order to allow mocking of this provider's okta based
// behavior for testing. In non test cases this hook will be the `useOktaAuth` hook from
// `okta-react`
const SessionProvider = ({
    children,
    oktaHook,
}: React.PropsWithChildren<ISessionProviderProps>) => {
    const { authState } = oktaHook();
    const store = useSessionStorage();
    const memberships = useOktaMemberships(authState?.accessToken);

    return (
        <SessionContext.Provider
            value={{
                oktaToken: authState?.accessToken,
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
