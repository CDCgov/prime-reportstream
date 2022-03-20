/* I put this in the `/contexts` directory because it felt
 * right, but by no means is it a context. Feel free to move
 * it if you can think of a better spot. */
export enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
    SENDER_MODE = "global-sender-mode",
    OKTA_ACCESS_TOKEN = "global-okta-token",
}

export function clearGlobalContext(): void {
    for (let key in sessionStorage) {
        sessionStorage.removeItem(key);
    }
}

export function parseOrgs() {
    // TODO: Parse orgs into objects with { org: string, sender?: string }
}

export function getStoredOktaToken(): string {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN) || "";
}

export function setStoredOktaToken(value: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN, value);
}

export function getStoredOrg(): string {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || "";
}

export function setStoredOrg(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, val);
}

export function getStoredSenderMode(): string {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.SENDER_MODE) || "";
}

export function setSenderMode(val: "P" | "T") {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.SENDER_MODE, val);
}
