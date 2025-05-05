import { AccessToken } from "@okta/okta-auth-js";
import { isValidElement } from "react";

import { RouteObject } from "react-router";
import { getOktaGroups, RSOrgType, RSUserClaims } from "./OrganizationUtils";
import { PERMISSIONS } from "./UsefulTypes";

const isSender = (s: string) => s.toLowerCase().includes(RSOrgType.SENDER);
const isAdmin = (s: string) => s.toLowerCase().includes(RSOrgType.ADMIN);
const isReceiver = (s: string) => !isSender(s) && !isAdmin(s);

/* Checks a user's Okta group memberships for any membership that qualifies
 * them to access what's behind the auth wall.
 *
 * Example: User is member of "DHSender_xx-phd" and "DHxx_phd". User will be
 * admitted to both sender and receiver features. */
const permissionCheck = (level: PERMISSIONS, token: AccessToken | undefined): boolean => {
    if (!token) return false;
    const oktaGroups = getOktaGroups(token);
    switch (level) {
        case PERMISSIONS.SENDER:
            return oktaGroups.map((org) => isSender(org)).includes(true);
        case PERMISSIONS.RECEIVER:
            return oktaGroups.map((org) => isReceiver(org)).includes(true);
        case PERMISSIONS.PRIME_ADMIN:
            return oktaGroups.map((org) => isAdmin(org)).includes(true);
    }
};

export interface RSUserPermissions {
    isUserAdmin: boolean;
    isUserSender: boolean;
    isUserReceiver: boolean;
    isUserTransceiver: boolean;
}

export function getUserPermissions(user?: RSUserClaims): RSUserPermissions {
    let isUserAdmin = false,
        isUserReceiver = false,
        isUserSender = false,
        isUserTransceiver = false;
    for (const org of user?.organization ?? []) {
        if (isAdmin(org)) isUserAdmin = true;
        if (isReceiver(org)) isUserReceiver = true;
        if (isSender(org)) isUserSender = true;
        if (isReceiver(org) && isSender(org)) isUserTransceiver = true;
    }

    return {
        isUserAdmin,
        isUserReceiver,
        isUserSender,
        isUserTransceiver,
    };
}

export const isAuthenticatedPath = (pathname: string, appRoutes: RouteObject[]) => {
    const basePath = pathname.split("/")[1];
    const matchedRoute = appRoutes[0].children?.find((route) => {
        return route.path?.includes(basePath);
    });

    if (!matchedRoute || !isValidElement(matchedRoute.element)) {
        return false;
    }

    return !!matchedRoute.element.props?.auth;
};

export { isSender, isReceiver, isAdmin, permissionCheck };
