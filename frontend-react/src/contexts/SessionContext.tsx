import React, {
    ReactNode,
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import { AccessToken, CustomUserClaims, UserClaims } from "@okta/okta-auth-js";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { useOktaAuth } from "@okta/okta-react";

import {
    MembershipSettings,
    MemberType,
    membershipsFromToken,
} from "../hooks/UseOktaMemberships";
import {
    getUserPermissions,
    RSUserPermissions,
} from "../utils/PermissionsUtils";
import { RSUserClaims } from "../utils/OrganizationUtils";
import config from "../config";
import { updateApiSessions } from "../network/Apis";

export interface RSSessionContext extends RSUserPermissions {
    activeMembership?: MembershipSettings | null;
    _activeMembership?: MembershipSettings | null;
    oktaToken?: Partial<AccessToken>;
    isAdminStrictCheck?: boolean;
    isUserAdmin: boolean;
    isUserSender: boolean;
    isUserReceiver: boolean;
    user?: UserClaims<CustomUserClaims>;
    environment: string;
    logout: () => void;
    setActiveMembership: (value: Partial<MembershipSettings> | null) => void;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

const { APP_ENV = "production" } = config;

export const SessionContext = createContext<RSSessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    activeMembership: {} as MembershipSettings,
    isAdminStrictCheck: false,
    isUserAdmin: false,
    isUserSender: false,
    isUserReceiver: false,
    environment: APP_ENV,
    logout: () => void 0,
    setActiveMembership: () => void 0,
});

const SessionProvider = ({ children }: { children: ReactNode }) => {
    // HACK: empty object fallback to account for tests not being rendered in Security
    // will be fixed once all rendering is funneled through a custom renderer
    const { authState = {}, oktaAuth } = useOktaAuth() || {};
    const initActiveMembership = useRef(
        JSON.parse(
            sessionStorage.getItem("__deprecatedActiveMembership") ?? "null",
        ),
    );
    const [_activeMembership, setActiveMembership] = useState(
        initActiveMembership.current,
    );
    const activeMembership = useMemo(() => {
        const actualMembership = membershipsFromToken(
            authState?.idToken?.claims,
        );

        if (actualMembership == null || !authState?.isAuthenticated)
            return null;

        return { ...actualMembership, ...(_activeMembership ?? {}) };
    }, [authState, _activeMembership]);

    const logout = useCallback(async () => {
        try {
            await oktaAuth.signOut({
                postLogoutRedirectUri: `${window.location.origin}/logout/callback`,
            });
        } catch (e) {
            console.trace(e);
        }
    }, [oktaAuth]);

    const context = useMemo(() => {
        return {
            oktaToken: authState?.accessToken,
            activeMembership,
            /* This logic is a for when admins have other orgs present on their Okta claims
             * that interfere with the activeMembership.memberType "soft" check */
            isAdminStrictCheck:
                activeMembership?.memberType === MemberType.PRIME_ADMIN,
            user: authState?.idToken?.claims,
            ...getUserPermissions(
                authState?.accessToken?.claims as RSUserClaims,
            ),
            environment: APP_ENV,
            logout,
            _activeMembership,
            setActiveMembership,
        };
    }, [activeMembership, authState, logout, _activeMembership]);

    useEffect(() => {
        updateApiSessions({
            token: authState?.accessToken?.accessToken ?? "",
            organization: activeMembership?.parsedName,
        });
    }, [activeMembership, authState?.accessToken?.accessToken]);

    useEffect(() => {
        if (!activeMembership) {
            sessionStorage.removeItem("__deprecatedActiveMembership");
        } else {
            sessionStorage.setItem(
                "__deprecatedActiveMembership",
                JSON.stringify(activeMembership),
            );
        }
    }, [activeMembership]);

    useEffect(() => {
        sessionStorage.setItem(
            "__deprecatedFetchInit",
            JSON.stringify({
                token: authState?.accessToken?.accessToken,
                organization: activeMembership?.parsedName,
            }),
        );
    }, [activeMembership?.parsedName, authState?.accessToken?.accessToken]);

    return (
        <SessionContext.Provider value={context}>
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
