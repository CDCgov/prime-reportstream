import type { AccessToken, CustomUserClaims, UserClaims } from "@okta/okta-auth-js";

import { PERMISSIONS } from "./UsefulTypes";
import type { RSService } from "../config/endpoints/settings";

export enum RSOrgType {
    SENDER = "sender",
    RECEIVER = "receiver",
    ADMIN = "admin",
}

/* New claims not present in UserClaims need tobe added via
 * this interface so UserClaims can implement the fields. */
export interface RSExtraClaims extends CustomUserClaims {
    organization: string[];
}
export type RSUserClaims = UserClaims<RSExtraClaims>;
export const toRSClaims = (claims: UserClaims): RSUserClaims => {
    return claims as RSUserClaims;
};

export interface AccessTokenWithRSClaims extends AccessToken {
    claims: RSUserClaims;
}

/* Parses the array of organizations (strings) from an AccessToken */
export const getOktaGroups = (accessToken: AccessToken | undefined): string[] => {
    if (!accessToken?.claims) return [];
    return toRSClaims(accessToken.claims).organization || [];
};

/* Converts Okta group names to their respective organization name
 * counterparts that we store in the RS database */
export const parseOrgName = (group: string | undefined): string => {
    const senderPrefix = `${PERMISSIONS.SENDER}_`;
    const isStandardGroup = group?.startsWith("DH");
    const isSenderGroup = group?.startsWith(senderPrefix);
    /* Quick definitions:
     * Sender - has an OktaGroup name beginning with DHSender_
     * Receiver - has an OktaGroup name beginning with DHxx_ where xx is any state code
     * Non-standard - has no associated OktaGroup and is already the org value in the db table */
    const groupType = isStandardGroup ? (isSenderGroup ? "sender" : "receiver") : "non-standard";

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

const PRIME_ADMINS = "DHPrimeAdmins";
const PREFIX_SENDER = "DHSender_";
const PREFIX_GENERAL = "DH";

export enum MemberType {
    SENDER = "sender",
    RECEIVER = "receiver",
    PRIME_ADMIN = "prime-admin",
    NON_STAND = "non-standard",
}

export interface MembershipSettings {
    // The org header value
    parsedName: string;
    // The type of membership
    memberType: MemberType;
    // Optional service name (i.e. "elr", "default")
    service?: string;
    // List of available services for the current org
    allServices?: RSService[];
}

export const getTypeOfGroup = (org: string) => {
    const isStandardType = org.startsWith(PREFIX_GENERAL);
    const isSenderType = org.startsWith(PREFIX_SENDER);
    const isAdminType = org === PRIME_ADMINS;
    if (isStandardType) {
        if (isAdminType) {
            return MemberType.PRIME_ADMIN;
        }
        if (isSenderType) {
            return MemberType.SENDER;
        }
        return MemberType.RECEIVER;
    } else {
        return MemberType.NON_STAND;
    }
};

export const extractSenderName = (org: string) => org.split(".")?.[1] || undefined;

/** This method constructs membership settings
 * @remarks This will put you as a default sender if you are not in a specific sender group */
export const getSettingsFromOrganization = (org: string): MembershipSettings => {
    const parsedName = parseOrgName(org);
    const memberType = getTypeOfGroup(org);
    let senderName = extractSenderName(org);

    if (memberType === MemberType.SENDER && !senderName) {
        senderName = "default";
    }

    return {
        parsedName,
        memberType,
        service: senderName,
    };
};

export const membershipsFromToken = (claims: UserClaims = {} as UserClaims): MembershipSettings | null => {
    const rsClaims = toRSClaims(claims);
    // Check if we have any organization claims
    if (!rsClaims?.organization?.length) {
        return null;
    }
    const orgClaim = rsClaims.organization.find((org) => org === PRIME_ADMINS) ?? rsClaims.organization[0];
    return getSettingsFromOrganization(orgClaim);
};
