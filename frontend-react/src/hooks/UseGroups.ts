import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { useReducer } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import { useOktaAuth } from "@okta/okta-react";

import { getOktaGroups, parseOrgName } from "../utils/OrganizationUtils";

export enum MemberType {
    SENDER = "sender",
    RECEIVER = "receiver",
    PRIME_ADMIN = "prime-admin",
    NON_STAND = "non-standard",
}

export enum MembershipActionType {
    SWITCH = "switch",
}

interface MembershipSettings {
    // The org header value
    parsedName: string;
    // The type of membership
    memberType: MemberType;
}

interface MembershipState {
    active?: MembershipSettings;
    // Key is the OKTA group name, settings has parsedName
    memberships?: Map<string, MembershipSettings>;
}

interface MembershipAction {
    type: MembershipActionType;
    // Only need to pass name of an org to swap to
    payload: string;
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
export const membershipsFromAuth = (auth: IOktaContext): MembershipState => {
    // One big undefined check to see if we have what we need for the next line
    if (!auth.authState?.accessToken?.claims) {
        return defaultState;
    }
    const claimData: Map<string, MembershipSettings> = makeMembershipMap(
        auth.authState.accessToken
    );
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
                active: state.memberships?.get(payload) || state.active,
            };
        default:
            return state;
    }
};

export const useGroups = () => {
    const auth = useOktaAuth();
    const [state, dispatch] = useReducer(
        membershipReducer,
        membershipsFromAuth(auth)
    );

    return { state, dispatch };
};
