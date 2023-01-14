import React, { useEffect, useReducer, useMemo } from "react";
import { AccessToken, AuthState } from "@okta/okta-auth-js";
import omit from "lodash.omit";

import { getOktaGroups, parseOrgName } from "../utils/OrganizationUtils";
import {
    storeSessionMembershipState,
    getSessionMembershipState,
    storeOrganizationOverride,
    getOrganizationOverride,
} from "../utils/SessionStorageTools";
import { updateApiSessions } from "../network/Apis";
import { RSService } from "../config/endpoints/settings";

export enum MemberType {
    SENDER = "sender",
    RECEIVER = "receiver",
    PRIME_ADMIN = "prime-admin",
    NON_STAND = "non-standard",
}

export enum MembershipActionType {
    ADMIN_OVERRIDE = "override",
    RESET = "reset",
    SET_MEMBERSHIPS_FROM_TOKEN = "setMemberships",
    INITIALIZE = "initialize",
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

export interface MembershipState {
    // null here points specifically to an uninitialized state
    activeMembership?: MembershipSettings | null;
    // Key is the OKTA group name, settings has parsedName
    memberships?: Map<string, MembershipSettings>;
    initialized?: boolean;
}

export interface MembershipController {
    state: MembershipState;
    dispatch: React.Dispatch<MembershipAction>;
}

export interface MembershipAction {
    type: MembershipActionType;
    // Only need to pass name of an org to swap to
    payload?: string | AccessToken | Partial<MembershipSettings>;
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
        service: senderName,
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
    // note that active will be set to {} rather than undefined in most real world cases on initialization
    // see `calculateMembershipsWithOverride` for logic
    activeMembership: null,
    memberships: undefined,
    initialized: false,
};

export const membershipsFromToken = (
    token: AccessToken | undefined
): Partial<MembershipState> => {
    // One big undefined check to see if we have what we need for the next line
    if (!token?.claims) {
        return omit(defaultState, "initialized");
    }
    const claimData: Map<string, MembershipSettings> =
        makeMembershipMapFromToken(token);
    // Catch anyone with no claim data
    if (!claimData.size) {
        return omit(defaultState, "initialized");
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
export const calculateMembershipsWithOverride = (
    membershipState: MembershipState
): MembershipState => {
    const override = getOrganizationOverride();
    const activeMembership = override || membershipState?.activeMembership;
    return {
        ...membershipState,
        activeMembership,
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
        case MembershipActionType.SET_MEMBERSHIPS_FROM_TOKEN:
            const parsedMemberships = membershipsFromToken(
                payload as AccessToken
            );
            return {
                ...calculateMembershipsWithOverride(
                    parsedMemberships as MembershipState
                ),
                initialized: true,
            };
        case MembershipActionType.ADMIN_OVERRIDE:
            const newActive = {
                ...state.activeMembership,
                ...(payload as MembershipSettings),
            };
            const newState = {
                ...state,
                activeMembership: newActive,
            };
            storeOrganizationOverride(JSON.stringify(newActive));
            return newState;
        case MembershipActionType.INITIALIZE:
            return { ...state, initialized: true };
        case MembershipActionType.RESET:
            return { ...defaultState, initialized: state.initialized };
        default:
            return state;
    }
};

// try to read from existing stored state
// since this is only called on first render, initialized will always be `false`
// even though we are (needlessly) storing and receving initialized values from store
export const getInitialState = () => {
    const storedState = getSessionMembershipState();
    const storedStateWithOverride = calculateMembershipsWithOverride(
        storedState || {}
    );
    // ALWAYS setting the `initialized` flag to false on the first render because...
    // we are prioritizing consistency over minor performance gains. This will always be false
    // on render 1, and once the `authState` comes in from the oktaHook, we get render 2
    // which will set `initialized` to true (see the 2nd useEffect in the main hook body)
    return { ...defaultState, ...storedStateWithOverride, initialized: false };
};

export const membershipReducer = (
    state: MembershipState,
    action: MembershipAction
) => {
    const newState = calculateNewState(state, action);

    // with this, session storage will always mirror app state
    // KNOWN ISSUE: this will not effectively store membership data for later retrieval, as this data is treated as Map
    // We are not using membership data (only active membership) anywhere in the app that I can find, so not an immediate issue
    // TODO: refactor membership as an array, don't bother trying to store it, or build a serializer to deal with the Map
    storeSessionMembershipState(JSON.stringify(newState));

    // to keep any requests using Api.ts up to date with auth headers
    // TODO: remove when we remove Api.ts based implementations
    updateApiSessions();
    return newState;
};

export const useOktaMemberships = (
    authState: AuthState | null
): MembershipController => {
    const initialState = useMemo(() => getInitialState(), []);
    const [state, dispatch] = useReducer(membershipReducer, initialState);

    const token = authState?.accessToken;
    const organizations = authState?.accessToken?.claims?.organization;

    // any time a token is updated in a way that changes orgs, we want to update membership state
    // this would potentially happen on a new login
    // but could also happen in a token refresh scenario if a users organizations claim is updated while they are logged in
    // NOTE: we are letting this do the work of setting memberships on log in. The Login component
    // will not explicitly set memberships.
    useEffect(() => {
        if (!token || !organizations) {
            return;
        }
        dispatch({
            type: MembershipActionType.SET_MEMBERSHIPS_FROM_TOKEN,
            payload: token,
        });
        // here we are only concerned about changes to a users orgs / memberships
    }, [organizations, !!token]); // eslint-disable-line react-hooks/exhaustive-deps

    // any time a token change signifies a logout, we should clear our state and storage
    useEffect(() => {
        // if we're in an uninitialized state, but okta has loaded, set our initialized flag
        // this should always happen on the second render once the oktaHook initializes and sends
        // something through
        if (!state.initialized && !!authState) {
            dispatch({
                type: MembershipActionType.INITIALIZE,
            });
            return;
        }
        // any time a token change signifies a logout, we should clear our state and storage
        if (authState && !authState.isAuthenticated) {
            // clear override as well. this will error on json parse and result in {} being fed back on a read
            storeOrganizationOverride("");
            dispatch({ type: MembershipActionType.RESET });
        }
    }, [!!authState, authState?.isAuthenticated]); // eslint-disable-line react-hooks/exhaustive-deps

    return { state, dispatch };
};
