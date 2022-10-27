import React, { createContext, useContext, useMemo } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
} from "../hooks/UseOktaMemberships";

export interface RSSessionContext {
    memberships?: Map<string, MembershipSettings>;
    activeMembership?: MembershipSettings | null;
    oktaToken?: Partial<AccessToken>;
    dispatch: React.Dispatch<MembershipAction>;
    initialized: boolean;
    isAdminStrictCheck?: boolean;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<RSSessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    memberships: new Map(),
    activeMembership: {} as MembershipSettings,
    dispatch: () => {},
    initialized: false,
    isAdminStrictCheck: false,
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
        state: { memberships, activeMembership, initialized },
        dispatch,
    } = useOktaMemberships(authState);
    /* This logic is a for when admins have other orgs present on their Okta claims
     * that interfere with the activeMembership.memberType "soft" check */
    const isAdminStrictCheck = useMemo(() => {
        if (initialized && memberships) {
            return memberships.has("DHPrimeAdmins");
        }
    }, [initialized, memberships]);

    return (
        <SessionContext.Provider
            value={{
                oktaToken: authState?.accessToken,
                memberships,
                activeMembership,
                isAdminStrictCheck,
                dispatch,
                initialized: authState !== null && !!initialized,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
