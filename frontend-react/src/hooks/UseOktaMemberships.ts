import React, { useEffect, useReducer } from "react";
import { AccessToken, AuthState } from "@okta/okta-auth-js";

import { getOktaGroups, parseOrgName } from "../utils/OrganizationUtils";
import {
    storeSessionMembershipState,
    getSessionMembershipState,
    storeOrganizationOverride,
    getOrganizationOverride,
} from "../contexts/SessionStorageTools";

export enum MemberType {
    SENDER = "sender",
    RECEIVER = "receiver",
    PRIME_ADMIN = "prime-admin",
    NON_STAND = "non-standard",
}

// TODO: update all member specific nomenclature to use SESSION
export enum MembershipActionType {
    // UPDATE_MEMBERSHIP = "updateMembership",
    ADMIN_OVERRIDE = "override",
    RESET = "reset",
    SET_MEMBERSHIPS = "setMemberships",
}

export interface MembershipSettings {
    // The org header value
    parsedName: string;
    // The type of membership
    memberType: MemberType;
    // Optional sender name
    senderName?: string;
}

export interface MembershipState {
    activeMembership?: MembershipSettings;
    // Key is the OKTA group name, settings has parsedName
    memberships?: Map<string, MembershipSettings>;
}

export interface MembershipController {
    state: MembershipState;
    dispatch: React.Dispatch<MembershipAction>;
}

export interface MembershipAction {
    type: MembershipActionType;
    // Only need to pass name of an org to swap to
    payload?: string | MembershipState | Partial<MembershipSettings>;
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

export const extractSenderName = (org: string) =>
    org.split(".")?.[1] || undefined;

/** This method constructs membership settings
 * @remarks This will put you as a default sender if you are not in a specific sender group */
export const getSettingsFromOrganization = (
    org: string
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
        senderName,
    };
};

export const makeMembershipMapFromToken = (
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
    activeMembership: undefined,
    memberships: undefined,
};
export const membershipsFromToken = (token: AccessToken): MembershipState => {
    // One big undefined check to see if we have what we need for the next line
    if (!token?.claims) {
        return defaultState;
    }
    const claimData: Map<string, MembershipSettings> =
        makeMembershipMapFromToken(token);
    // Catch anyone with no claim data
    if (!claimData.size) {
        return defaultState;
    }
    // Get defaults
    const [first] = claimData.keys();
    const active = claimData.get(first);
    return {
        activeMembership: active,
        memberships: claimData,
    };
};

// allows for overriding active membership with override previously set in session storage
const calculateNewMemberships = (
    updatedMembershipState: MembershipState
): MembershipState => {
    const override = getOrganizationOverride();
    return {
        ...updatedMembershipState,
        activeMembership: {
            ...updatedMembershipState.activeMembership,
            ...override,
        },
    };
};

// determines the new state and returns it
// this is most of the actual reducer logic
const calculateNewState = (
    state: MembershipState,
    action: MembershipAction
) => {
    const { type, payload } = action;
    switch (type) {
        case MembershipActionType.SET_MEMBERSHIPS:
            return calculateNewMemberships(payload as MembershipState);
        case MembershipActionType.ADMIN_OVERRIDE:
            const newState = {
                ...state,
                activeMembership: {
                    ...state.activeMembership,
                    ...(payload as MembershipSettings),
                },
            };
            storeOrganizationOverride(JSON.stringify(payload));
            return newState;
        case MembershipActionType.RESET:
            return defaultState;
        default:
            return state;
    }
};

// try to read from existing stored state
const getInitialState = () => {
    return { ...defaultState, ...getSessionMembershipState() };
};

export const membershipReducer = (
    state: MembershipState,
    action: MembershipAction
) => {
    const newState = calculateNewState(state, action);
    // with this, session storage will always mirror app state
    storeSessionMembershipState(JSON.stringify(newState));
    return newState;
};

export const useOktaMemberships = (
    // token: AccessToken | undefined
    authState: AuthState | null
): MembershipController => {
    const [state, dispatch] = useReducer(membershipReducer, getInitialState());

    const token = authState?.accessToken;

    // need to make sure this doesn't run on an infinite loop in a real world situation
    // may need to drill down on the dependency array if it does, or refactor this hook
    // to deal solely with claims rather than tokens.
    useEffect(() => {
        if (token) {
            dispatch({
                type: MembershipActionType.SET_MEMBERSHIPS,
                payload: membershipsFromToken(token),
            });
        }
        // this signifies a log out
        if (authState && !authState.isAuthenticated) {
            // clear override as well. this will error on json parse and result in {} being fed back on a read
            storeOrganizationOverride("");
            dispatch({ type: MembershipActionType.RESET });
        }
    }, [token?.claims]); // eslint-disable-line react-hooks/exhaustive-deps

    return { state, dispatch };
};
