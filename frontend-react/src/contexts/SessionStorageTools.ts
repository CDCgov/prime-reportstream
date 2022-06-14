/* I put this in the `/contexts` directory because it felt
 * right, but by no means is it a context. Feel free to move
 * it if you can think of a better spot. */

import { AxiosRequestHeaders } from "axios";

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

export function getStoredOrg(): string | undefined {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined;
}

export function setStoredOrg(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, val);
    updateApiSessions(headersFromStoredSession());
}

export function getStoredSenderName(): string | undefined {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.SENDER_NAME) || undefined;
}

export function setStoredSenderName(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.SENDER_NAME, val);
}
