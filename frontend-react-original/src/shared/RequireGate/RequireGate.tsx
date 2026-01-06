import { lazy, PropsWithChildren, ReactElement } from "react";
import { Navigate, useLocation } from "react-router";

import useFeatureFlags from "../../contexts/FeatureFlag/useFeatureFlags";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { FeatureFlagName } from "../../pages/misc/FeatureFlags";
import { PERMISSIONS } from "../../utils/UsefulTypes";

const ErrorNoPage = lazy(() => import("../../pages/error/legacy-content/ErrorNoPage"));

export interface RequireGateBaseProps extends PropsWithChildren {
    auth?: boolean | PERMISSIONS | PERMISSIONS[];
    featureFlags?: FeatureFlagName | FeatureFlagName[];
    failElement: ReactElement;
    anonymousElement: ReactElement;
}

/**
 * Component wrapper to enforce auth and feature flags requirements.
 */
export function RequireGateBase({ auth, children, featureFlags, anonymousElement, failElement }: RequireGateBaseProps) {
    const { authState } = useSessionContext();
    const { checkFlags } = useFeatureFlags();
    const perms = auth ? (Array.isArray(auth) ? auth : typeof auth === "boolean" ? [] : [auth]) : undefined;
    const flags = Array.isArray(featureFlags) ? featureFlags : featureFlags ? [featureFlags] : [];
    let isAdmin = false,
        isAuthAllowed = false,
        isFeatureAllowed = false;

    // feature flag check
    if (flags.length === 0 || checkFlags(flags)) {
        isFeatureAllowed = true;
    }

    // auth check
    // if no auth requirements or any auth required while logged in
    if (!perms || (perms.length === 0 && authState.isAuthenticated)) isAuthAllowed = true;
    else {
        // if anonymous, send to login
        if (!authState.isAuthenticated) {
            return anonymousElement;
        }
        const match = (authState.accessToken?.claims.organization as PERMISSIONS[] | undefined)?.find((g) =>
            perms.find((t) => {
                if (g === PERMISSIONS.PRIME_ADMIN) {
                    isAdmin = true;
                    return g === t;
                }

                return g.startsWith(t);
            }),
        );
        if (match) isAuthAllowed = true;
    }

    // if any checks fail, render 404
    if (!isAdmin && (!isAuthAllowed || !isFeatureAllowed)) {
        return failElement;
    }

    return <>{children}</>;
}

export type RequireGateProps = Omit<RequireGateBaseProps, "anonymousElement" | "failElement">;

export function RequireGate(props: RequireGateProps) {
    const location = useLocation();
    return (
        <RequireGateBase
            {...props}
            anonymousElement={<Navigate to="/login" replace state={{ originalUrl: location.pathname }} />}
            failElement={<ErrorNoPage />}
        />
    );
}

export default RequireGate;
