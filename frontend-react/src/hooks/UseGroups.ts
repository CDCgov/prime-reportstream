import React, { useReducer } from "react";
import { AccessToken } from "@okta/okta-auth-js";

import { getOktaGroups, parseOrgName } from "../utils/OrganizationUtils";

export enum MemberType {
    SENDER = "sender",
    RECEIVER = "receiver",
    PRIME_ADMIN = "prime-admin",
    NON_STAND = "non-standard",
}

export enum MembershipActionType {
    SWITCH = "switch",
    UPDATE = "update",
}

interface MembershipSettings {
    // The org header value
    parsedName: string;
    // The type of membership
    memberType: MemberType;
}

export interface MembershipState {
    active?: MembershipSettings;
    // Key is the OKTA group name, settings has parsedName
    memberships?: Map<string, MembershipSettings>;
}

export interface MembershipController {
    state: MembershipState;
    dispatch: React.Dispatch<MembershipAction>;
}

interface MembershipAction {
    type: MembershipActionType;
    // Only need to pass name of an org to swap to
    payload: string | AccessToken;
}

export const getTypeOfGroup = (org: string) => {
    const isStandardType = org.startsWith("DH");
    const isSenderType = org.startsWith("DHSender_");
    const isAdminType = org === "DHPrimeAdmins";
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

export const getSettingsFromOrganization = (
    org: string
): MembershipSettings => {
    return {
        parsedName: parseOrgName(org),
        memberType: getTypeOfGroup(org),
    };
};

export const makeMembershipMap = (
    token: AccessToken
): Map<string, MembershipSettings> => {
    // Extracts claims from token
    const organizationClaim = getOktaGroups(token);
    const settings: Map<string, MembershipSettings> = new Map();
    // Creates map from claims
    organizationClaim.forEach((org: string) => {
        settings.set(org, getSettingsFromOrganization(org));
    });
    return settings;
};

const defaultState: MembershipState = {
    active: undefined,
    memberships: undefined,
};
export const membershipsFromAuth = (token: AccessToken): MembershipState => {
    // One big undefined check to see if we have what we need for the next line
    if (!token?.claims) {
        return defaultState;
    }
    const claimData: Map<string, MembershipSettings> = makeMembershipMap(token);
    // Catch anyone with no claim data
    if (!claimData.size) {
        return defaultState;
    }
    // Get defaults
    const [first] = claimData.keys();
    const active = claimData.get(first);
    return {
        active: active,
        memberships: claimData,
    };
};

export const membershipReducer = (
    state: MembershipState,
    action: MembershipAction
) => {
    const { type, payload } = action;
    switch (type) {
        case MembershipActionType.SWITCH:
            return {
                ...state,
                active:
                    state.memberships?.get(payload as string) || state.active,
            };
        case MembershipActionType.UPDATE:
            return membershipsFromAuth(payload as AccessToken);
        default:
            return state;
    }
};

export const useGroups = (): MembershipController => {
    const [state, dispatch] = useReducer(membershipReducer, defaultState);

    return { state, dispatch };
};
