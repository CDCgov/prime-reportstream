/* I put this in the `/contexts` directory because it felt
 * right, but by no means is it a context. Feel free to move
 * it if you can think of a better spot. */

import { AxiosRequestHeaders } from "axios";
import {
    MembershipSettings,
    MembershipState,
} from "../hooks/UseOktaMemberships";

import { updateApiSessions } from "../network/Apis";

const headersFromStoredSession = (): AxiosRequestHeaders => ({
    Authorization: `Bearer ${getStoredOktaToken() || ""}`,
    // TODO: make sure weird API edge case for `updateApiSessions` is handled
    Organization: getStoredOrg() || "",
});

export enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
    SENDER_NAME = "global-sender-name",
    OKTA_ACCESS_TOKEN = "global-okta-token",
    MEMBERSHIP_STATE = "global-membership-state",
    ORGANIZATION_OVERRIDE = "global-organization-override",
}

export function getStoredOktaToken(): string | undefined {
    return (
        sessionStorage.getItem(GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN) ||
        undefined
    );
}

export function setStoredOktaToken(value: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN, value);
    updateApiSessions(headersFromStoredSession());
}

// TODO: refactor this out of existing code
export function getStoredOrg(): string | undefined {
    console.log(
        "~~~ read session org",
        sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined
    );
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined;
}

// TODO: refactor this out of existing code
export function setStoredOrg(val: string) {
    console.log("!!! set in session", val);
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, val);
    updateApiSessions(headersFromStoredSession());
}

// export function getStoredSenderName(): string | undefined {
//     return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.SENDER_NAME) || undefined;
// }

// export function setStoredSenderName(val: string) {
//     sessionStorage.setItem(GLOBAL_STORAGE_KEYS.SENDER_NAME, val);
// }

const fetchJsonFromSession = (storageKey: string) => {
    const storedString = sessionStorage.getItem(storageKey);
    if (!storedString) {
        return {};
    }
    try {
        const sessionJson = JSON.parse(storedString);
        return sessionJson;
    } catch {
        return {};
    }
};

// not sure this is actually necessary. Okta should handle refresh of non-admin related state
export function getOrganizationOverride(): MembershipSettings {
    return fetchJsonFromSession(GLOBAL_STORAGE_KEYS.ORGANIZATION_OVERRIDE);
}

export function storeOrganizationOverride(override: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.ORGANIZATION_OVERRIDE, override);
}

// not sure this is actually necessary. Okta should handle refresh of non-admin related state
export function getSessionMembershipState(): MembershipState {
    return fetchJsonFromSession(GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE);
}

export function storeSessionMembershipState(membershipState: string) {
    sessionStorage.setItem(
        GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE,
        membershipState
    );
}
