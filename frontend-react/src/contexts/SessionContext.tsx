import React, {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import OktaAuth, {
    AccessToken,
    CustomUserClaims,
    UserClaims,
    AuthState,
} from "@okta/okta-auth-js";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { Security, useOktaAuth } from "@okta/okta-react";

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
import { OKTA_AUTH } from "../oktaConfig";

export interface RSSessionContext extends RSUserPermissions {
    oktaAuth: OktaAuth;
    authState: AuthState;
    activeMembership?: MembershipSettings;
    _activeMembership?: MembershipSettings;
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
    oktaAuth: OKTA_AUTH,
    oktaToken: {} as Partial<AccessToken>,
    activeMembership: {} as MembershipSettings,
    isAdminStrictCheck: false,
    isUserAdmin: false,
    isUserSender: false,
    isUserReceiver: false,
    environment: APP_ENV,
    logout: () => void 0,
    setActiveMembership: () => void 0,
} as any);

export interface SessionProviderProps
    extends React.ComponentProps<typeof Security> {}

function SessionProvider({ children, ...props }: SessionProviderProps) {
    return (
        <Security {...props}>
            <SessionAuthStateGate>{children}</SessionAuthStateGate>
        </Security>
    );
}

function SessionAuthStateGate({ children }: React.PropsWithChildren) {
    const { authState, ...props } = useOktaAuth();

    if (!authState) return null;

    return (
        <SessionProviderBase authState={authState} {...props}>
            {children}
        </SessionProviderBase>
    );
}

export interface SessionProviderBaseProps
    extends React.PropsWithChildren<
        Omit<ReturnType<typeof useOktaAuth>, "authState">
    > {
    authState: AuthState;
}

export function SessionProviderBase({
    children,
    oktaAuth,
    authState,
}: SessionProviderBaseProps) {
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

        if (actualMembership == null || !authState.isAuthenticated)
            return undefined;

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
            oktaAuth,
            authState,
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
    }, [oktaAuth, authState, activeMembership, logout, _activeMembership]);

    useEffect(() => {
        updateApiSessions({
            token: authState?.accessToken?.accessToken ?? "",
            organization: activeMembership?.parsedName,
        });
    }, [activeMembership?.parsedName, authState?.accessToken?.accessToken]);

    useEffect(() => {
        if (!authState.isAuthenticated && _activeMembership) {
            setActiveMembership(undefined);
        }

        if (!activeMembership) {
            sessionStorage.removeItem("__deprecatedActiveMembership");
            sessionStorage.removeItem("__deprecatedFetchInit");
        } else {
            sessionStorage.setItem(
                "__deprecatedActiveMembership",
                JSON.stringify(activeMembership),
            );
            sessionStorage.setItem(
                "__deprecatedFetchInit",
                JSON.stringify({
                    token: authState?.accessToken?.accessToken,
                    organization: activeMembership?.parsedName,
                }),
            );
        }
    }, [_activeMembership, activeMembership, authState]);

    useEffect(() => {}, [authState]);

    return (
        <SessionContext.Provider value={context}>
            {children}
        </SessionContext.Provider>
    );
}

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
