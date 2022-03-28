import { AccessToken } from "@okta/okta-auth-js";

import { getOktaGroups, RSOrgType } from "./OrganizationUtils";

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

const permissionCheck = (
    level: PERMISSIONS,
    token: AccessToken | undefined
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

export { PERMISSIONS, isSender, isReceiver, isAdmin, permissionCheck };
