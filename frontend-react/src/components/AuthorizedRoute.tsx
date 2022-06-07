// @ts-nocheck // TODO: fix types in this file
import React from "react";
import { Redirect } from "react-router-dom";
import { SecureRoute, useOktaAuth } from "@okta/okta-react";

import { PERMISSIONS, permissionCheck } from "../utils/PermissionsUtils";

export const AuthorizedRoute = ({
    component: Component,
    authorize: permission,
    ...rest
}) => {
    const { authState } = useOktaAuth();
    const adminOverride = permissionCheck(
        PERMISSIONS.PRIME_ADMIN,
        authState?.accessToken
    );
    return (
        <SecureRoute
            {...rest}
            render={(props) => {
                /* PERMISSIONS REFACTOR: This should use memberships for
                 * access checking */
                if (
                    authState?.accessToken &&
                    (adminOverride ||
                        permissionCheck(permission, authState.accessToken))
                ) {
                    // permission authorized -> render component
                    return <Component {...props} />;
                }

                // permission not authorized so redirect to home page
                return <Redirect to={{ pathname: "/" }} />;
            }}
        />
    );
};
