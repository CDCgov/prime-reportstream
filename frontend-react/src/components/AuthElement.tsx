import { useNavigate } from "react-router-dom";
import React, { useEffect, useMemo } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { MemberType } from "../hooks/UseOktaMemberships";
import { FeatureFlagName } from "../pages/misc/FeatureFlags";
import { useFeatureFlags } from "../contexts/FeatureFlagContext";

import Spinner from "./Spinner";

interface AuthElementProps {
    element: JSX.Element;
    requiredUserType?: MemberType | MemberType[];
    requiredFeatureFlag?: FeatureFlagName;
}
/** Wrap elements with required feature flag and/or user type checks */
export const AuthElement = ({
    element,
    requiredUserType,
    requiredFeatureFlag,
}: AuthElementProps): React.ReactElement => {
    // Router's new navigation hook for redirecting
    const navigate = useNavigate();
    const { oktaToken, activeMembership, initialized, isAdminStrictCheck } =
        useSessionContext();

    const { checkFlag } = useFeatureFlags();

    // This memberType won't require an undefined fallback when used like the one
    // from useSessionContext
    const memberType = useMemo(
        () => activeMembership?.memberType || MemberType.NON_STAND,
        [activeMembership]
    );
    // Dynamically authorize user from single or multiple allowed user types
    const authorizeMemberType = useMemo(() => {
        if (isAdminStrictCheck || memberType === MemberType.PRIME_ADMIN)
            return true; // Admin authentication always
        return requiredUserType instanceof Array
            ? requiredUserType.includes(memberType)
            : requiredUserType === memberType;
    }, [isAdminStrictCheck, memberType, requiredUserType]);
    // All the checks before returning the route

    const needsLogin = useMemo(
        () => !oktaToken || !activeMembership,
        [oktaToken, activeMembership]
    );
    useEffect(() => {
        // not ready to make a determination about auth status yet, show a spinner
        if (!initialized) {
            return;
        }
        // Not logged in, needs to log in.
        if (needsLogin) {
            navigate("/login");
            return;
        }
        // Not authorized as current member type
        if (requiredUserType && !authorizeMemberType) {
            navigate("/");
            return;
        }
        // Does not have feature flag enabled
        if (requiredFeatureFlag && !checkFlag(requiredFeatureFlag)) {
            navigate("/");
        }
    }, [
        activeMembership,
        authorizeMemberType,
        navigate,
        requiredFeatureFlag,
        requiredUserType,
        needsLogin,
        initialized,
        checkFlag,
    ]);

    const elementToRender = useMemo(
        () => (initialized ? element : <Spinner />),
        [initialized, element]
    );
    return elementToRender;
};
