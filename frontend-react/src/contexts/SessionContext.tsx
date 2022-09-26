import React, { createContext, useContext } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
} from "../hooks/UseOktaMemberships";

export interface ISessionContext {
    memberships?: Map<string, MembershipSettings>;
    activeMembership?: MembershipSettings;
    oktaToken?: Partial<AccessToken>;
    dispatch: React.Dispatch<MembershipAction>;
    initialized: boolean;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<ISessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    memberships: new Map(),
    activeMembership: {} as MembershipSettings,
    dispatch: () => {},
    initialized: false,
});

// accepts `oktaHook` as a parameter in order to allow mocking of this provider's okta based
// behavior for testing. In non test cases this hook will be the `useOktaAuth` hook from
// `okta-react`
const SessionProvider = ({
    children,
    oktaHook,
}: React.PropsWithChildren<ISessionProviderProps>) => {
    const { authState } = oktaHook();

    const {
        state: { memberships, activeMembership },
        dispatch,
    } = useOktaMemberships(authState);
    return (
        <SessionContext.Provider
            value={{
                oktaToken: authState?.accessToken,
                memberships,
                activeMembership,
                dispatch,
                initialized: authState !== null,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
