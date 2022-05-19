import { AccessToken, UserClaims } from "@okta/okta-auth-js";

import { SessionStore } from "../hooks/UseSessionStorage";

import { isAdmin, isReceiver, isSender, PERMISSIONS } from "./PermissionsUtils";

enum RSOrgType {
    SENDER = "sender",
    RECEIVER = "receiver",
    ADMIN = "admin",
}

/* New claims not present in UserClaims need tobe added via
 * this interface so UserClaims can implement the fields. */
interface RSExtraClaims {
    organization: string[];
}
type RSUserClaims = UserClaims<RSExtraClaims>;
const toRSClaims = (claims: UserClaims): RSUserClaims => {
    return claims as RSUserClaims;
};

/* Parses the array of organizations (strings) from an AccessToken */
const getOktaGroups = (accessToken: AccessToken | undefined): string[] => {
    if (!accessToken?.claims) return [];
    return toRSClaims(accessToken.claims).organization || [];
};

/* Converts Okta group names to their respective organization name
 * counterparts that we store in the RS database */
const groupToOrg = (group: string | undefined): string => {
    const senderPrefix = `${PERMISSIONS.SENDER}_`;
    const isStandardGroup = group?.startsWith("DH");
    const isSenderGroup = group?.startsWith(senderPrefix);
    /* Quick definitions:
     * Sender - has an OktaGroup name beginning with DHSender_
     * Receiver - has an OktaGroup name beginning with DHxx_ where xx is any state code
     * Non-standard - has no associated OktaGroup and is already the org value in the db table */
    const groupType = isStandardGroup
        ? isSenderGroup
            ? "sender"
            : "receiver"
        : "non-standard";

    switch (groupType) {
        case "sender":
            // DHSender_test_sender -> test_sender
            // DHSender_another-test-sender -> another-test-sender
            return group ? group.replace(senderPrefix, "") : "";
        case "receiver":
            // DHmd_phd -> md-phd
            return group ? group.replace("DH", "").replace(/_/g, "-") : "";
        case "non-standard":
            // simple_report -> simple_report
            return group ? group : "";
    }
};

/* Delivers the array of organization names, or the first, parsed from Okta
 * groups. */
const getRSOrgs = (
    token: AccessToken | undefined,
    onlyType?: RSOrgType
): string[] | string => {
    if (!token || !token.claims) return [];
    /* Have to convert to RSUserClaims type to get Organization claim */
    const oktaGroups = getOktaGroups(token);

    /* Consolidate all the filtering logic */
    const filterGroups = (): string[] => {
        switch (onlyType) {
            /* Return those WITH sender */
            case RSOrgType.SENDER:
                return oktaGroups.filter((group) => isSender(group));
            /* Return those WITHOUT sender */
            case RSOrgType.RECEIVER:
                return oktaGroups.filter((group) => isReceiver(group));
            /* Return ONLY admin groups */
            case RSOrgType.ADMIN:
                return oktaGroups.filter((group) => isAdmin(group));
            /* Return all groups */
            default:
                return oktaGroups;
        }
    };
    return filterGroups().map((group) => groupToOrg(group));
};

function parseOrgs(orgs: Array<string>): Array<Partial<SessionStore>> {
    return orgs.map((org) => {
        // Org names are case sensitive. This condition will fail if the okta
        // group name is not cased properly: DHSender_xyz, DHxy_phd, DHPrimeAdmin
        if (org.includes(PERMISSIONS.SENDER)) {
            const sender = org.split(".");
            return {
                org: groupToOrg(sender[0]),
                senderName: sender[1] || "default",
            };
        } else {
            return {
                org: groupToOrg(org),
                senderName: undefined,
            };
        }
    });
}

export { RSOrgType, getOktaGroups, groupToOrg, getRSOrgs, parseOrgs };

export type { RSUserClaims };
