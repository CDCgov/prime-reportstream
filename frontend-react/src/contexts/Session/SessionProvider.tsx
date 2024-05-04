import {
    AuthState,
    CustomUserClaims,
    OktaAuth,
    UserClaims,
} from "@okta/okta-auth-js";
import { useOktaAuth } from "@okta/okta-react";
import { AxiosRequestConfig } from "axios";
import {
    createContext,
    PropsWithChildren,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";

import { IIdleTimerProps, useIdleTimer } from "react-idle-timer";
import type { AppConfig } from "../../config";
import site from "../../content/site.json";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext";
import { updateApiSessions } from "../../network/Apis";
import { OpenAPI } from "../../utils/Api";
import { EventName } from "../../utils/AppInsights";
import { isUseragentPreferred } from "../../utils/BrowserUtils";
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
import { RSConsole } from "../../utils/rsConsole/rsConsole";

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

export const SessionContext = createContext<RSSessionContext>(null as any);

export interface SessionProviderProps extends PropsWithChildren {
    config: AppConfig;
    rsConsole: RSConsole;
}

function SessionProvider({
    children,
    config,
    rsConsole,
}: SessionProviderProps) {
    OpenAPI.BASE = config.API_ROOT
    const { authState, oktaAuth } = useOktaAuth();
    const aiReactPlugin = useAppInsightsContext();

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

        if (actualMembership == null || !authState?.isAuthenticated)
            return undefined;

        return { ...actualMembership, ...(_activeMembership ?? {}) };
    }, [authState, _activeMembership]);

    /**
     * Keep our auth header middleware up-to-date
     */
    useEffect(() => {
        const authInterceptorFn = (req: AxiosRequestConfig<any>) => {
            req.headers = {
                ...req.headers,
                "x-ms-session-id": aiReactPlugin.properties.context.getSessionId(),
                "authentication-type": "okta",
                authorization: `Bearer ${
                    authState?.accessToken?.accessToken ?? ""
                }`,
                organization: `${activeMembership?.parsedName ?? ""}`,
            }

            return req;
        }
        OpenAPI.interceptors.request.use(authInterceptorFn);

        return () => OpenAPI.interceptors.request.eject(authInterceptorFn)
    }, [activeMembership?.parsedName, aiReactPlugin.properties.context, authState?.accessToken?.accessToken])

    const logout = useCallback(async () => {
        try {
            await oktaAuth.signOut({
                postLogoutRedirectUri: `${window.location.origin}/`,
            });
        } catch (e) {
            rsConsole.warn("Failed to logout", e);
        }
    }, [oktaAuth, rsConsole]);

    const handleIdle = useCallback<
        Exclude<IIdleTimerProps["onIdle"], undefined>
    >(
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
        onIdle: () => void handleIdle(),
        ...config.IDLE_TIMERS,
    });

    const sessionStartTime = useRef<number>(new Date().getTime());
    const sessionTimeAggregate = useRef<number>(0);
    const calculateAggregateTime = () => {
        return (
            new Date().getTime() -
            sessionStartTime.current +
            sessionTimeAggregate.current
        );
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

    const context = useMemo(() => {
        return {
            oktaAuth,
            authState,
            activeMembership,
            user: {
                claims: authState?.idToken?.claims,
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

    useEffect(() => {
        aiReactPlugin.customProperties.activeMembership = activeMembership;
    }, [activeMembership, aiReactPlugin]);

    // keep auth user up-to-date
    useEffect(() => {
        if (
            authState?.idToken?.claims.email &&
            !aiReactPlugin.properties.context.user.authenticatedId
        ) {
            aiReactPlugin.properties.context.user.setAuthenticatedUserContext(
                authState.idToken.claims.email,
                undefined,
                true,
            );
        } else if (
            !authState?.idToken?.claims.email &&
            aiReactPlugin.properties.context.user.authenticatedId
        ) {
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

    return (
        <SessionContext.Provider value={context as RSSessionContext}>
            {children}
        </SessionContext.Provider>
    );
}

export default SessionProvider;
