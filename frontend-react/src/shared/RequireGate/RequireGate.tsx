import { Navigate, useLocation } from "react-router";
import React from "react";

import { useSessionContext } from "../../contexts/Session";
import { FeatureFlagName } from "../../pages/misc/FeatureFlags";
import { useFeatureFlags } from "../../contexts/FeatureFlags";
import { MemberType, RSUser } from "../../utils/OrganizationUtils";

const ErrorNoPage = React.lazy(
    () => import("../../pages/error/legacy-content/ErrorNoPage"),
);

export interface RequireGateBaseProps extends React.PropsWithChildren {
    user: RSUser;
    checkFlags: (flags: FeatureFlagName[]) => boolean;
    auth?: boolean | MemberType | MemberType[];
    featureFlags?: FeatureFlagName | FeatureFlagName[];
    failElement: React.ReactElement;
    anonymousElement: React.ReactElement;
}

/**
 * Component wrapper to enforce auth and feature flags requirements.
 */
export function RequireGateBase({
    user,
    checkFlags,
    auth,
    children,
    featureFlags,
    anonymousElement,
    failElement,
}: RequireGateBaseProps) {
    const perms = auth
        ? Array.isArray(auth)
            ? auth
            : typeof auth === "boolean"
            ? []
            : [auth]
        : undefined;
    const flags = Array.isArray(featureFlags)
        ? featureFlags
        : featureFlags
        ? [featureFlags]
        : [];
    let isAdmin = false,
        isAuthAllowed = false,
        isFeatureAllowed = false;

    // feature flag check
    if (flags.length === 0 || checkFlags(flags)) {
        isFeatureAllowed = true;
    }

    // auth check
    // if no auth requirements or any auth required while logged in
    if (!perms || (perms.length === 0 && !user.isAnonymous))
        isAuthAllowed = true;
    else {
        // if anonymous, send to login
        if (user.isAnonymous) {
            return anonymousElement;
        }
        if (user.isAdmin) isAdmin = true;
        if (user.isAdmin || perms.every((a) => user.hasAssocationType(a)))
            isAuthAllowed = true;
    }

    // if any checks fail, render 404
    if (!isAdmin && (!isAuthAllowed || !isFeatureAllowed)) {
        return failElement;
    }

    return <>{children}</>;
}

export interface RequireGateProps
    extends Omit<RequireGateBaseProps, "anonymousElement" | "failElement"> {}

export function RequireGate(props: RequireGateProps) {
    const { checkFlags } = useFeatureFlags();
    const location = useLocation();
    const { user } = useSessionContext();
    return (
        <RequireGateBase
            {...props}
            user={user}
            checkFlags={checkFlags}
            anonymousElement={
                <Navigate
                    to="/login"
                    replace
                    state={{ originalUrl: location.pathname }}
                />
            }
            failElement={<ErrorNoPage />}
        />
    );
}

export default RequireGate;
