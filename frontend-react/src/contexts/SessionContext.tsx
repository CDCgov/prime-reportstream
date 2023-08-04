import React, { ReactNode, createContext, useContext, useMemo } from "react";
import { AccessToken, CustomUserClaims, UserClaims } from "@okta/okta-auth-js";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { useOktaAuth } from "@okta/okta-react";

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
import config from "../config";

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
    environment: string;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

const { APP_ENV = "production" } = config;

export const SessionContext = createContext<RSSessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    activeMembership: {} as MembershipSettings,
    dispatch: () => {},
    initialized: false,
    isAdminStrictCheck: false,
    isUserAdmin: false,
    isUserSender: false,
    isUserReceiver: false,
    environment: APP_ENV,
});

const SessionProvider = ({ children }: { children: ReactNode }) => {
    // HACK: empty object fallback to account for tests not being rendered in Security
    // will be fixed once all rendering is funneled through a custom renderer
    const oktaAuth = useOktaAuth() || {};
    const { authState } = oktaAuth;
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
                authState?.accessToken?.claims as RSUserClaims,
            ),
            environment: APP_ENV,
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
