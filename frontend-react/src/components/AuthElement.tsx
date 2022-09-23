import { useNavigate } from "react-router-dom";
import React, { useEffect, useMemo } from "react";

import Spinner from "./Spinner";
import { useSessionContext } from "../contexts/SessionContext";
import { MemberType } from "../hooks/UseOktaMemberships";
import { FeatureFlagName } from "../pages/misc/FeatureFlags";
import { useFeatureFlags } from "../contexts/FeatureFlagContext";

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
    const { oktaToken, activeMembership, initialized } = useSessionContext();

    const { checkFlag } = useFeatureFlags();

    // This memberType won't require an undefined fallback when used like the one
    // from useSessionContext
    const memberType = useMemo(
        () => activeMembership?.memberType || MemberType.NON_STAND,
        [activeMembership]
    );
    // Dynamically authorize user from single or multiple allowed user types
    const authorizeMemberType = useMemo(() => {
        if (memberType === MemberType.PRIME_ADMIN) return true; // Admin authentication always
        return requiredUserType instanceof Array
            ? requiredUserType.includes(memberType)
            : requiredUserType === memberType;
    }, [requiredUserType, memberType]);
    // All the checks before returning the route
    useEffect(() => {
        // not ready to make a determination about auth status yet, show a spinner
        if (!initialized) {
            return;
        }
        // Not logged in, needs to log in.
        if (!oktaToken || !activeMembership) {
            console.log("*** initialized but", oktaToken, activeMembership);
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
        oktaToken?.accessToken,
        requiredFeatureFlag,
        requiredUserType,
        !!oktaToken,
        initialized,
        checkFlag,
    ]);

    const elementToRender = useMemo(
        () => (initialized ? element : <Spinner />),
        [initialized]
    );
    return elementToRender;
};
