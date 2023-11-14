import { useOktaAuth } from "@okta/okta-react";
import { useRef, useState, useMemo, useCallback, useEffect } from "react";
import type { AuthState } from "@okta/okta-auth-js";

import { AppConfig } from "../../config";
import { updateApiSessions } from "../../network/Apis";
import {
    MembershipSettings,
    membershipsFromToken,
    RSUserClaims,
    MemberType,
} from "../../utils/OrganizationUtils";
import { getUserPermissions } from "../../utils/PermissionsUtils";
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
            rsconsole,
        };
    }, [
        oktaAuth,
        authState,
        activeMembership,
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
