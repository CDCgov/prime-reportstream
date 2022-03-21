/* I put this in the `/contexts` directory because it felt
 * right, but by no means is it a context. Feel free to move
 * it if you can think of a better spot. */
import { PERMISSIONS } from "../resources/PermissionsResource";
import { groupToOrg } from "../webreceiver-utils";

export enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
    SENDER_MODE = "global-sender-mode",
    SENDER_NAME = "global-sender-name",
    OKTA_ACCESS_TOKEN = "global-okta-token",
}

export function clearGlobalContext(): void {
    for (let key in sessionStorage) {
        sessionStorage.removeItem(key);
    }
}

export function parseOrgs(orgs: Array<string>) {
    // TODO: Parse orgs into objects with { org: string, sender?: string }
    return orgs.map((org) => {
        if (org.includes(PERMISSIONS.SENDER)) {
            const sender = org.split(".");
            return {
                org: groupToOrg(sender[0]),
                sender: sender[1] || "default",
            };
        } else {
            return {
                org: groupToOrg(org),
                sender: undefined,
            };
        }
    });
}

export function storeParsedOrg(parsedVal: {
    org: string;
    sender: string | undefined;
}) {
    // Sets az-phd as default when Admins have no state or sender orgs
    if (parsedVal.org.includes("PrimeAdmin")) {
        setStoredOrg("az-phd");
    } else {
        setStoredOrg(parsedVal.org);
        setStoredSenderName(parsedVal.sender || "");
    }
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

export function setSenderMode(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.SENDER_MODE, val);
}

export function getStoredSenderName(): string {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.SENDER_NAME) || "";
}

export function setStoredSenderName(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.SENDER_NAME, val);
}
