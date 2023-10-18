import type { UserClaims } from "@okta/okta-auth-js";

import { parseOrgName, toRSClaims } from "../utils/OrganizationUtils";
import { RSService } from "../config/endpoints/settings";

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

export const extractSenderName = (org: string) =>
    org.split(".")?.[1] || undefined;

/** This method constructs membership settings
 * @remarks This will put you as a default sender if you are not in a specific sender group */
export const getSettingsFromOrganization = (
    org: string,
): MembershipSettings => {
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

export const membershipsFromToken = (
    claims: UserClaims = {} as UserClaims,
): MembershipSettings | null => {
    const rsClaims = toRSClaims(claims);
    // Check if we have any organization claims
    if (!rsClaims?.organization?.length) {
        return null;
    }
    const orgClaim =
        rsClaims.organization.find((org) => org === PRIME_ADMINS) ??
        rsClaims.organization[0];
    return getSettingsFromOrganization(orgClaim);
};
