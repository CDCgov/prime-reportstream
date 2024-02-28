import {
    AuthState,
    CustomUserClaims,
    OktaAuth,
    UserClaims,
} from "@okta/okta-auth-js";
import { Security, useOktaAuth } from "@okta/okta-react";
import {
    ComponentProps,
    createContext,
    PropsWithChildren,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";

import type { AppConfig } from "../../config";
import site from "../../content/site.json";
import { updateApiSessions } from "../../network/Apis";
import { RSConsole } from "../../utils/console";
import {
    MembershipSettings,
    membershipsFromToken,
    MemberType,
    RSUserClaims,
} from "../../utils/OrganizationUtils";
import {
    getUserPermissions,
    RSUserPermissions,
} from "../../utils/PermissionsUtils";
import { useAppInsightsContext } from "../AppInsights";

export interface RSSessionContext {
    oktaAuth: OktaAuth;
    authState: AuthState;
    activeMembership?: MembershipSettings;
    _activeMembership?: MembershipSettings;
    user: {
        claims?: UserClaims<CustomUserClaims>;
        isAdminStrictCheck: boolean;
        isUserTransceiver: boolean;
    } & RSUserPermissions;
    logout: () => void;
    setActiveMembership: (value: Partial<MembershipSettings> | null) => void;
    config: AppConfig;
    site: typeof site;
    rsConsole: RSConsole;
}

export const SessionContext = createContext<RSSessionContext>({
    activeMembership: {} as MembershipSettings,
    logout: () => void 0,
    setActiveMembership: () => void 0,
} as any);

export interface SessionProviderProps extends ComponentProps<typeof Security> {
    config: AppConfig;
}

function SessionProvider({ children, config, ...props }: SessionProviderProps) {
    return (
        <Security {...props}>
            <SessionAuthStateGate config={config}>
                {children}
            </SessionAuthStateGate>
        </Security>
    );
}

export type SessionAuthStateGateProps = PropsWithChildren<
    Pick<SessionProviderProps, "config">
>;

function SessionAuthStateGate({ children, config }: SessionAuthStateGateProps) {
    const { authState, ...props } = useOktaAuth();

    if (!authState) return null;

    return (
        <SessionProviderBase authState={authState} config={config} {...props}>
            {children}
        </SessionProviderBase>
    );
}

export interface SessionProviderBaseProps
    extends PropsWithChildren<
        Omit<ReturnType<typeof useOktaAuth>, "authState">
    > {
    authState: AuthState;
    config: AppConfig;
}

export function SessionProviderBase({
    children,
    oktaAuth,
    authState,
    config,
}: SessionProviderBaseProps) {
    const { appInsights } = useAppInsightsContext();
    const initActiveMembership = useRef(
        JSON.parse(
            sessionStorage.getItem("__deprecatedActiveMembership") ?? "null",
        ),
    );
    const [_activeMembership, setActiveMembership] = useState(
        initActiveMembership.current,
    );
    const activeMembership = useMemo<MembershipSettings | undefined>(() => {
        const actualMembership = membershipsFromToken(
            authState?.accessToken?.claims,
        );

        if (actualMembership == null || !authState.isAuthenticated)
            return undefined;

        return { ...actualMembership, ...(_activeMembership ?? {}) };
    }, [authState, _activeMembership]);

    const rsConsole = useMemo(
        () =>
            new RSConsole({
                ai: appInsights?.sdk,
                consoleSeverityLevels: config.AI_CONSOLE_SEVERITY_LEVELS,
                reportableConsoleLevels: config.AI_REPORTABLE_CONSOLE_LEVELS,
                env: config.MODE,
            }),
        [
            appInsights,
            config.AI_CONSOLE_SEVERITY_LEVELS,
            config.AI_REPORTABLE_CONSOLE_LEVELS,
            config.MODE,
        ],
    );

    const logout = useCallback(async () => {
        try {
            await oktaAuth.signOut({
                postLogoutRedirectUri: `${window.location.origin}/`,
            });
        } catch (e) {
            rsConsole.warn("Failed to logout", e);
        }
    }, [oktaAuth, rsConsole]);

    const context = useMemo(() => {
        return {
            oktaAuth,
            authState,
            activeMembership,
            user: {
                claims: authState.idToken?.claims,
                ...getUserPermissions(
                    authState?.accessToken?.claims as RSUserClaims,
                ),
                /* This logic is a for when admins have other orgs present on their Okta claims
                 * that interfere with the activeMembership.memberType "soft" check */
                isAdminStrictCheck:
                    activeMembership?.memberType === MemberType.PRIME_ADMIN,
            },
            logout,
            _activeMembership,
            setActiveMembership,
            config,
            site,
            rsConsole,
        };
    }, [
        oktaAuth,
        authState,
        activeMembership,
        logout,
        _activeMembership,
        config,
        rsConsole,
    ]);

    useEffect(() => {
        updateApiSessions({
            token: authState.accessToken?.accessToken ?? "",
            organization: activeMembership?.parsedName ?? "",
        });
    }, [activeMembership?.parsedName, authState.accessToken?.accessToken]);

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

    useEffect(() => void 0, [authState]);

    return (
        <SessionContext.Provider value={context}>
            {children}
        </SessionContext.Provider>
    );
}

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
