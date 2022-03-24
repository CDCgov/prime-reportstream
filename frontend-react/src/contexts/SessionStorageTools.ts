/* I put this in the `/contexts` directory because it felt
 * right, but by no means is it a context. Feel free to move
 * it if you can think of a better spot. */
import { PERMISSIONS } from "../resources/PermissionsResource";
import { groupToOrg } from "../webreceiver-utils";
import { SessionStore } from "../hooks/UseSessionStorage";

export enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
    SENDER_NAME = "global-sender-name",
    OKTA_ACCESS_TOKEN = "global-okta-token",
}

export function parseOrgs(orgs: Array<string>): Array<Partial<SessionStore>> {
    return orgs.map((org) => {
        // Org names are case sensitive. This condition will fail if the okta
        // group name is not cased properly: DHSender_xyz, DHxy_phd, DHPrimeAdmin
        if (org.includes(PERMISSIONS.SENDER)) {
            const sender = org.split(".");
            return {
                org: groupToOrg(sender[0]),
                senderName: sender[1] || "default",
            };
        } else {
            return {
                org: groupToOrg(org),
                senderName: undefined,
            };
        }
    });
}

export function getStoredOktaToken(): string | undefined {
    return (
        sessionStorage.getItem(GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN) ||
        undefined
    );
}

export function setStoredOktaToken(value: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN, value);
}

export function getStoredOrg(): string | undefined {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || undefined;
}

export function setStoredOrg(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, val);
}

export function getStoredSenderName(): string | undefined {
    return sessionStorage.getItem(GLOBAL_STORAGE_KEYS.SENDER_NAME) || undefined;
}

export function setStoredSenderName(val: string) {
    sessionStorage.setItem(GLOBAL_STORAGE_KEYS.SENDER_NAME, val);
}
