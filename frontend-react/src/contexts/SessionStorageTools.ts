/* I put this in the `/contexts` directory because it felt
 * right, but by no means is it a context. Feel free to move
 * it if you can think of a better spot. */

import { AxiosRequestHeaders } from "axios";
import { MembershipState } from "../hooks/UseOktaMemberships";

import { updateApiSessions } from "../network/Apis";

const headersFromStoredSession = (): AxiosRequestHeaders => ({
    Authorization: `Bearer ${getStoredOktaToken() || ""}`,
    Organization: getStoredOrg() || "",
});

export enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
    SENDER_NAME = "global-sender-name",
    OKTA_ACCESS_TOKEN = "global-okta-token",
    MEMBERSHIP_STATE = "global-membership-state",
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

// TODO: use this to pull data from membership state
export function getStoredOrg(): string | undefined {
    console.log(
        "~~~ read session org",
        sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined
    );
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined;
}

export function setStoredOrg(val: string) {
    console.log("!!! set in session", val);
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, val);
    updateApiSessions(headersFromStoredSession());
}

export function getStoredSenderName(): string | undefined {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.SENDER_NAME) || undefined;
}

export function setStoredSenderName(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.SENDER_NAME, val);
}

export function getSessionMembershipState(): MembershipState {
    const stateString = sessionStorage.getItem(
        GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE
    );
    if (!stateString) {
        return {};
    }
    try {
        const stateJson = JSON.parse(stateString);
        return stateJson;
    } catch {
        return {};
    }
    // return (
    //     sessionStorage.getItem(GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE) ||
    //     undefined
    // );
}

export function setSessionMembershipState(membershipState: string) {
    sessionStorage.setItem(
        GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE,
        membershipState
    );
}
