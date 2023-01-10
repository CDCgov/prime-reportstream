import React, { createContext, useContext, useMemo } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
    MemberType,
} from "../hooks/UseOktaMemberships";

export interface RSSessionContext {
    /* TODO: Remove - we do not anticipate a need for one user having multiple Okta groups */
    memberships?: Map<string, MembershipSettings>;
    activeMembership?: MembershipSettings | null;
    oktaToken?: Partial<AccessToken>;
    dispatch: React.Dispatch<MembershipAction>;
    initialized: boolean;
    /* TODO: Remove - see comment on L50-51 */
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
    /* TODO: Remove - logic can be easily checked by accessing the session context
     *   and confirming memberType from any component */
    const isAdminStrictCheck = useMemo(() => {
        return activeMembership?.memberType === MemberType.PRIME_ADMIN;
    }, [activeMembership?.memberType]);

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
