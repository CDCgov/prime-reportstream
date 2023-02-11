import React, { createContext, useContext, useMemo } from "react";
import { AccessToken, CustomUserClaims, UserClaims } from "@okta/okta-auth-js";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
    MemberType,
} from "../hooks/UseOktaMemberships";
import {
    getUserPermissions,
    RSUserPermissions,
} from "../utils/PermissionsUtils";
import { RSUserClaims } from "../utils/OrganizationUtils";

export interface RSSessionContext extends RSUserPermissions {
    activeMembership?: MembershipSettings | null;
    oktaToken?: Partial<AccessToken>;
    dispatch: React.Dispatch<MembershipAction>;
    initialized: boolean;
    isAdminStrictCheck?: boolean;
    isUserAdmin: boolean;
    isUserSender: boolean;
    isUserReceiver: boolean;
    user?: UserClaims<CustomUserClaims>;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<RSSessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    activeMembership: {} as MembershipSettings,
    dispatch: () => {},
    initialized: false,
    isAdminStrictCheck: false,
    isUserAdmin: false,
    isUserSender: false,
    isUserReceiver: false,
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
        state: { activeMembership, initialized },
        dispatch,
    } = useOktaMemberships(authState);

    const context = useMemo(() => {
        return {
            oktaToken: authState?.accessToken,
            activeMembership,
            /* This logic is a for when admins have other orgs present on their Okta claims
             * that interfere with the activeMembership.memberType "soft" check */
            isAdminStrictCheck:
                activeMembership?.memberType === MemberType.PRIME_ADMIN,
            dispatch,
            initialized: authState !== null && !!initialized,
            user: authState?.idToken?.claims,
            ...getUserPermissions(
                authState?.accessToken?.claims as RSUserClaims
            ),
        };
    }, [activeMembership, authState, dispatch, initialized]);

    return (
        <SessionContext.Provider value={context}>
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
