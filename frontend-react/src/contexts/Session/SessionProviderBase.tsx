import { useOktaAuth } from "@okta/okta-react";
import { useRef, useState, useMemo, useCallback, useEffect } from "react";
import type { AuthState } from "@okta/okta-auth-js";
import { useNavigate } from "react-router";

import { AppConfig } from "../../config";
import { updateApiSessions } from "../../network/Apis";
import {
    MembershipSettings,
    membershipsFromToken,
    RSUserClaims,
    RSUser,
    UserAssociation,
} from "../../utils/OrganizationUtils";
import { RSConsole } from "../../utils/console";
import { useAppInsightsContext } from "../AppInsights";
import site from "../../content/site.json";

import { SessionContext } from ".";

export interface SessionProviderBaseProps
    extends React.PropsWithChildren<
        Omit<ReturnType<typeof useOktaAuth>, "authState">
    > {
    authState: AuthState;
    config: AppConfig;
}

export default function SessionProviderBase({
    children,
    oktaAuth,
    authState,
    config,
}: SessionProviderBaseProps) {
    const navigate = useNavigate();
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

    const rsconsole = useMemo(
        () =>
            new RSConsole({
                ai: appInsights?.sdk,
                consoleSeverityLevels: config.AI_CONSOLE_SEVERITY_LEVELS,
                reportableConsoleLevels: config.AI_REPORTABLE_CONSOLE_LEVELS,
                env: config.CLIENT_ENV,
            }),
        [
            appInsights,
            config.AI_CONSOLE_SEVERITY_LEVELS,
            config.AI_REPORTABLE_CONSOLE_LEVELS,
            config.CLIENT_ENV,
        ],
    );

    const logout = useCallback(async () => {
        try {
            await oktaAuth.signOut({
                postLogoutRedirectUri: `${window.location.origin}/`,
            });
        } catch (e) {
            rsconsole.warn("Failed to logout", e);
        }
    }, [oktaAuth, rsconsole]);

    const user = useMemo(
        () =>
            new RSUser({
                claims: authState.idToken?.claims as RSUserClaims | undefined,
            }),
        [authState.idToken?.claims],
    );

    const [impersonatedUser, setImpersonatedUser] = useState<
        RSUser | undefined
    >();
    const impersonate = useCallback(
        (value: UserAssociation | UserAssociation[]) => {
            setImpersonatedUser(new RSUser({ impersonation: value }));
            navigate("/");
        },
        [navigate],
    );

    const context = useMemo(() => {
        return {
            oktaAuth,
            authState,
            activeMembership,
            _user: user,
            impersonatedUser,
            user: impersonatedUser ?? user,
            impersonate,
            clearImpersonation: () => setImpersonatedUser(undefined),
            logout,
            _activeMembership,
            setActiveMembership,
            config,
            site,
            rsconsole,
        };
    }, [
        oktaAuth,
        authState,
        activeMembership,
        user,
        impersonatedUser,
        impersonate,
        logout,
        _activeMembership,
        config,
        rsconsole,
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

    useEffect(() => {}, [authState]);

    return (
        <SessionContext.Provider value={context}>
            {children}
        </SessionContext.Provider>
    );
}
