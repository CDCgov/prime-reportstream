import { AuthState, CustomUserClaims, OktaAuth, UserClaims } from "@okta/okta-auth-js";
import { useOktaAuth } from "@okta/okta-react";
import axios, { AxiosError } from "axios";
import { createContext, PropsWithChildren, useCallback, useEffect, useMemo, useRef, useState } from "react";

import { IIdleTimerProps, useIdleTimer } from "react-idle-timer";
import type { AppConfig } from "../../config";
import { AxiosOptionsWithSegments, RSEndpoint } from "../../config/endpoints";
import site from "../../content/site.json";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import { updateApiSessions } from "../../network/Apis";
import { EventName } from "../../utils/AppInsights";
import { isUseragentPreferred } from "../../utils/BrowserUtils";
import { MembershipSettings, membershipsFromToken, MemberType, RSUserClaims } from "../../utils/OrganizationUtils";
import { getUserPermissions, RSUserPermissions } from "../../utils/PermissionsUtils";
import { RSConsole } from "../../utils/rsConsole/rsConsole";
import { RSNetworkError } from "../../utils/RSNetworkError";

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
    authorizedFetch: <T = any>(options: Partial<AxiosOptionsWithSegments>, EndpointConfig?: RSEndpoint) => Promise<T>;
}

export const SessionContext = createContext<RSSessionContext>(null as any);

export interface SessionProviderProps extends PropsWithChildren {
    config: AppConfig;
    rsConsole: RSConsole;
}

export interface StaticAuthorizedFetchProps {
    apiUrl: string;
    sessionId?: string;
    accessToken?: string;
    organization?: string;
    options: Partial<AxiosOptionsWithSegments>;
    endpointConfig?: RSEndpoint;
}

export async function staticAuthorizedFetch<T = unknown>({
    apiUrl,
    sessionId = "",
    accessToken = "",
    organization = "",
    options,
    endpointConfig,
}: StaticAuthorizedFetchProps) {
    if (options.segments && !endpointConfig) throw new Error("EndpointConfig required when using segments");
    if (options.url && endpointConfig) throw new Error("Cannot use both url and EndpointConfig");
    if (!options.url && !endpointConfig) throw new Error("Must use either url or EndpointConfig");

    const headerOverrides = options?.headers ?? {};

    const authHeaders = {
        "x-ms-session-id": sessionId,
        "authentication-type": "okta",
        authorization: `Bearer ${accessToken}`,
        organization: organization,
    };
    const headers = { ...authHeaders, ...headerOverrides };

    const axiosConfig = endpointConfig
        ? endpointConfig.toAxiosConfig({
              ...options,
              headers,
          })
        : { ...options, headers };

    // Add base url if needed
    if (!endpointConfig) {
        if (axiosConfig.url?.startsWith("/")) {
            axiosConfig.url = `${apiUrl}${options.url}`;
        }
    }

    try {
        const res = await axios<T>(axiosConfig);
        return res.data;
    } catch (e: any) {
        if (e instanceof AxiosError) {
            throw new RSNetworkError(e);
        }
        throw e;
    }
}

function SessionProvider({ children, config, rsConsole }: SessionProviderProps) {
    const { authState, oktaAuth } = useOktaAuth();
    const aiReactPlugin = useAppInsightsContext();

    const initActiveMembership = useRef(JSON.parse(sessionStorage.getItem("__deprecatedActiveMembership") ?? "null"));
    const [_activeMembership, setActiveMembership] = useState(initActiveMembership.current);
    const activeMembership = useMemo<MembershipSettings | undefined>(() => {
        const actualMembership = membershipsFromToken(authState?.accessToken?.claims);

        if (actualMembership == null || !authState?.isAuthenticated) return undefined;

        return { ...actualMembership, ...(_activeMembership ?? {}) };
    }, [authState, _activeMembership]);

    const logout = useCallback(async () => {
        try {
            await oktaAuth.signOut({
                postLogoutRedirectUri: `${window.location.origin}/`,
            });
        } catch (e) {
            rsConsole.warn("Failed to logout", e);
        }
    }, [oktaAuth, rsConsole]);

    const handleIdle = useCallback<Exclude<IIdleTimerProps["onIdle"], undefined>>(
        // eslint-disable-next-line @typescript-eslint/no-misused-promises
        async (_event, _timer) => {
            if (await oktaAuth.isAuthenticated()) {
                aiReactPlugin.trackEvent({
                    name: EventName.SESSION_DURATION,
                    properties: {
                        sessionLength: sessionTimeAggregate.current / 1000,
                    },
                });
                await logout();
            }
        },
        [logout, aiReactPlugin, oktaAuth],
    );

    useIdleTimer({
        onIdle: () => handleIdle(),
        ...config.IDLE_TIMERS,
    });

    const sessionStartTime = useRef<number>(new Date().getTime());
    const sessionTimeAggregate = useRef<number>(0);
    const calculateAggregateTime = () => {
        return new Date().getTime() - sessionStartTime.current + sessionTimeAggregate.current;
    };

    // do best-attempt window tracking
    useEffect(() => {
        const onUnload = () => {
            aiReactPlugin.trackEvent({
                name: EventName.SESSION_DURATION,
                properties: {
                    sessionLength: calculateAggregateTime() / 1000,
                },
            });
        };
        const onVisibilityChange = () => {
            if (document.visibilityState === "hidden") {
                sessionTimeAggregate.current = calculateAggregateTime();
            } else if (document.visibilityState === "visible") {
                sessionStartTime.current = new Date().getTime();
            }
        };
        window.addEventListener("beforeunload", onUnload);
        window.addEventListener("visibilitychange", onVisibilityChange);

        return () => {
            window.removeEventListener("beforeunload", onUnload);
            window.removeEventListener("visibilitychange", onVisibilityChange);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Authorized fetcher that handles headers and supports manual url or EndpointConfig
    const authorizedFetch = useCallback(
        async function <TData>(
            options: Partial<AxiosOptionsWithSegments>,
            endpointConfig?: RSEndpoint,
        ): Promise<TData> {
            return staticAuthorizedFetch({
                apiUrl: config.API_ROOT,
                accessToken: authState?.accessToken?.accessToken,
                endpointConfig,
                options,
                organization: activeMembership?.parsedName,
                sessionId: aiReactPlugin.properties.context.getSessionId(),
            });
        },
        [
            activeMembership?.parsedName,
            aiReactPlugin.properties.context,
            authState?.accessToken?.accessToken,
            config.API_ROOT,
        ],
    );

    const context = useMemo(() => {
        return {
            oktaAuth,
            authState,
            activeMembership,
            user: {
                claims: authState?.idToken?.claims,
                ...getUserPermissions(authState?.accessToken?.claims as RSUserClaims),
                /* This logic is a for when admins have other orgs present on their Okta claims
                 * that interfere with the activeMembership.memberType "soft" check */
                isAdminStrictCheck: activeMembership?.memberType === MemberType.PRIME_ADMIN,
            },
            logout,
            _activeMembership,
            setActiveMembership,
            config,
            site,
            rsConsole,
            authorizedFetch,
        };
    }, [oktaAuth, authState, activeMembership, logout, _activeMembership, config, rsConsole, authorizedFetch]);

    useEffect(() => {
        updateApiSessions({
            token: authState?.accessToken?.accessToken ?? "",
            organization: activeMembership?.parsedName ?? "",
        });
    }, [activeMembership?.parsedName, authState?.accessToken?.accessToken]);

    useEffect(() => {
        if (!authState?.isAuthenticated && _activeMembership) {
            setActiveMembership(undefined);
        }

        if (!activeMembership) {
            sessionStorage.removeItem("__deprecatedActiveMembership");
            sessionStorage.removeItem("__deprecatedFetchInit");
        } else {
            sessionStorage.setItem("__deprecatedActiveMembership", JSON.stringify(activeMembership));
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

    useEffect(() => {
        aiReactPlugin.customProperties.activeMembership = activeMembership;
    }, [activeMembership, aiReactPlugin]);

    // keep auth user up-to-date
    useEffect(() => {
        if (authState?.idToken?.claims.email && !aiReactPlugin.properties.context.user.authenticatedId) {
            aiReactPlugin.properties.context.user.setAuthenticatedUserContext(
                authState.idToken.claims.email,
                undefined,
                true,
            );
        } else if (!authState?.idToken?.claims.email && aiReactPlugin.properties.context.user.authenticatedId) {
            aiReactPlugin.properties.context.user.clearAuthenticatedUserContext();
        }
    }, [authState?.idToken, aiReactPlugin]);

    // Mark that user agent is outdated on telemetry for filtering
    useEffect(() => {
        if (!isUseragentPreferred(window.navigator.userAgent))
            aiReactPlugin.customProperties.isUserAgentOutdated = true;
        else aiReactPlugin.customProperties.isUserAgentOutdated = undefined;
    }, [aiReactPlugin]);

    if (!authState) return null;

    return <SessionContext.Provider value={context as RSSessionContext}>{children}</SessionContext.Provider>;
}

export default SessionProvider;
