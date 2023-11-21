import type {
    AccessToken,
    CustomUserClaims,
    UserClaims,
} from "@okta/okta-auth-js";

import type { RSService } from "../config/endpoints/settings";

import { PERMISSIONS } from "./UsefulTypes";

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
export const getOktaGroups = (
    accessToken: AccessToken | undefined,
): string[] => {
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

const PRIME_ADMINS = "DHPrimeAdmins";
const PREFIX_SENDER = "DHSender_";
const PREFIX_GENERAL = "DH";

export enum MemberType {
    SENDER = "sender",
    RECEIVER = "receiver",
    PRIME_ADMIN = "prime-admin",
    NON_STAND = "non-standard",
    ORG_ADMIN = "org-admin",
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

/**
 * TODO: Revamp users in okta.
 * - Move group name array from `organization` to `groups` in claims
 * - Record user's display-friendly organization name in their profiles
 * - Research and determine best way (if any) to represent an Okta user
 *   that can have multiple associations spanning multiple organizations
 */
export function parseAssociations(claims: RSUserClaims) {
    if (!claims.organization) throw new Error("No organization claim found");

    // organization claim is actually array of group names
    return claims.organization.map((groupName) => {
        const type = getTypeOfGroup(groupName),
            organizationId =
                type === MemberType.PRIME_ADMIN
                    ? "reportstream"
                    : parseOrgName(groupName),
            name = extractSenderName(groupName);

        return {
            organizationId,
            type,
            name,
        };
    });
}

export interface UserAssociation {
    organizationId: string;
    type: MemberType;
    name?: string;
}

export type UserOrgAssociationQuery = Pick<UserAssociation, "organizationId"> &
    Partial<Omit<UserAssociation, "organizationId">>;

export class RSUser {
    associations: UserAssociation[];
    isImpersonated: boolean;
    isAnonymous: boolean;

    organization?: string;
    email?: string;
    username?: string;
    familyName?: string;
    givenName?: string;

    constructor({
        claims,
        impersonation: _impersonation,
    }: {
        claims?: RSUserClaims;
        impersonation?: UserAssociation | UserAssociation[];
    }) {
        const impersonation = Array.isArray(_impersonation)
            ? _impersonation
            : _impersonation
            ? [_impersonation]
            : undefined;

        this.associations = claims
            ? parseAssociations(claims)
            : impersonation ?? [];
        this.isAnonymous = !impersonation && !claims;
        this.isImpersonated = !!impersonation;

        this.organization =
            impersonation?.[0].organizationId ??
            this.associations?.[0]?.organizationId;
        this.email = claims?.email;
        this.username =
            impersonation?.[0].name ??
            claims?.preferred_username ??
            claims?.email;
        this.familyName = claims?.family_name;
        this.givenName = claims?.given_name;
    }

    getOrgAssociations({
        organizationId,
        type,
        name,
    }: UserOrgAssociationQuery) {
        return this.associations.filter(
            (a) =>
                a.organizationId === organizationId &&
                (!type || a.type === type) &&
                (!name || a.name === name),
        );
    }

    hasOrgAssociation(query: UserOrgAssociationQuery) {
        return this.getOrgAssociations(query).length > 0;
    }

    hasAssocationType(type: MemberType) {
        return this.associations.some((a) => a.type === type);
    }

    get isAdmin() {
        return this.associations.some((a) => a.type === MemberType.PRIME_ADMIN);
    }

    get isSender() {
        return this.associations.some((a) => a.type === MemberType.SENDER);
    }

    get isReceiver() {
        return this.associations.some((a) => a.type === MemberType.RECEIVER);
    }

    get isTransceiver() {
        return this.isSender && this.isReceiver;
    }

    isOrgAdmin(orgId: string) {
        return this.associations.some(
            (a) =>
                a.type === MemberType.ORG_ADMIN && a.organizationId === orgId,
        );
    }

    // Deprecated
    get isUserAdmin() {
        return this.isAdmin;
    }
    get isAdminStrictCheck() {
        return this.isAdmin;
    }
    get isUserSender() {
        return this.isSender;
    }
    get isUserReceiver() {
        return this.isReceiver;
    }
    get isUserTransceiver() {
        return this.isTransceiver;
    }
}
