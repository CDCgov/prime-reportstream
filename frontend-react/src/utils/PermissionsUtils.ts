import { AccessToken } from "@okta/okta-auth-js";

import { getOktaGroups, RSOrgType, RSUserClaims } from "./OrganizationUtils";

enum PERMISSIONS {
    /* Non-Okta pseudo groups */
    SENDER = "DHSender",
    RECEIVER = "DHReceiver",
    /* Okta groups */
    PRIME_ADMIN = "DHPrimeAdmins",
}

const isSender = (s: string) => s.toLowerCase().includes(RSOrgType.SENDER);
const isAdmin = (s: string) => s.toLowerCase().includes(RSOrgType.ADMIN);
const isReceiver = (s: string) => !isSender(s) && !isAdmin(s);

/* Checks a user's Okta group memberships for any membership that qualifies
 * them to access what's behind the auth wall.
 *
 * Example: User is member of "DHSender_xx-phd" and "DHxx_phd". User will be
 * admitted to both sender and receiver features. */
const permissionCheck = (
    level: PERMISSIONS,
    token: AccessToken | undefined,
): boolean => {
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
}

export function getUserPermissions(user?: RSUserClaims): RSUserPermissions {
    let isUserAdmin = false,
        isUserReceiver = false,
        isUserSender = false;
    for (const org of user?.organization ?? []) {
        if (isAdmin(org)) isUserAdmin = true;
        if (isReceiver(org)) isUserReceiver = true;
        if (isSender(org)) isUserSender = true;
    }

    return {
        isUserAdmin,
        isUserReceiver,
        isUserSender,
    };
}

export { PERMISSIONS, isSender, isReceiver, isAdmin, permissionCheck };
