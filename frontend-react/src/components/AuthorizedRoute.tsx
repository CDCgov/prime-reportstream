// @ts-nocheck // TODO: fix types in this file
import React from "react";
import { Redirect } from "react-router-dom";
import { SecureRoute, useOktaAuth } from "@okta/okta-react";

import { permissionCheck } from "../webreceiver-utils";
import { PERMISSIONS } from "../resources/PermissionsResource";

export const AuthorizedRoute = ({
    component: Component,
    authorize: permission,
    ...rest
}) => {
    const { authState } = useOktaAuth();
    return (
        <SecureRoute
            {...rest}
            render={(props) => {
                /* TODO: Refactor this to support many args when refactoring permissions layer! */

                if (
                    permissionCheck(PERMISSIONS.PRIME_ADMIN, authState) ||
                    permissionCheck(permission, authState)
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
