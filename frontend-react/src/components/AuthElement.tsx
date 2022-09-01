import { useNavigate } from "react-router-dom";
import React, { useCallback, useEffect, useMemo } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { MemberType } from "../hooks/UseOktaMemberships";
import { CheckFeatureFlag, FeatureFlagName } from "../pages/misc/FeatureFlags";

interface AuthElementProps {
    element: () => JSX.Element;
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
    const { oktaToken, activeMembership } = useSessionContext();
    // This memberType won't require an undefined fallback when used like the one
    // from useSessionContext
    const memberType = useMemo(
        () => activeMembership?.memberType || MemberType.NON_STAND,
        [activeMembership]
    );
    // Dynamically authorize user from single or multiple allowed user types
    const authorizeMemberType = useCallback(() => {
        if (memberType === MemberType.PRIME_ADMIN) return true; // Admin authentication always
        return requiredUserType instanceof Array
            ? requiredUserType.includes(memberType)
            : requiredUserType === memberType;
    }, [memberType]); // eslint-disable-line
    // All the checks before returning the route
    useEffect(() => {
        if (!oktaToken?.accessToken || !activeMembership) navigate("/login"); // Not logged in, needs to log in.
        if (requiredUserType && !authorizeMemberType()) navigate("/"); // Not authorized as current member type
        if (requiredFeatureFlag && !CheckFeatureFlag(requiredFeatureFlag))
            navigate("/"); // Does not have feature flag enabled
    }, [
        activeMembership,
        authorizeMemberType,
        navigate,
        oktaToken?.accessToken,
        requiredFeatureFlag,
        requiredUserType,
    ]);

    return element(); // Checks passed, render page
};
