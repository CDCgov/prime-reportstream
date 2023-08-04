/* I put this in the `/contexts` directory because it felt
 * right, but by no means is it a context. Feel free to move
 * it if you can think of a better spot. */

import {
    MembershipSettings,
    MembershipState,
} from "../hooks/UseOktaMemberships";

export enum GLOBAL_STORAGE_KEYS {
    GLOBAL_BASE = "global-context-",
    GLOBAL_ORG = "global-context-org",
    SENDER_NAME = "global-sender-name",
    OKTA_ACCESS_TOKEN = "okta-token-storage", // set by okta, should probably read from localStorage instead
    MEMBERSHIP_STATE = "global-membership-state",
    ORGANIZATION_OVERRIDE = "global-organization-override",
}

/* feature flags are just and array of strings saved into a single localStorage variable */
const FEATURE_FLAG_LOCALSTORAGE_KEY = "featureFlags";

const fetchJsonFromStorage = (storageKey: string) => {
    const storedString = localStorage.getItem(storageKey);
    if (!storedString) {
        return;
    }
    try {
        const storageJson = JSON.parse(storedString);
        return storageJson;
    } catch {
        console.info("Error reading json from storage at key - ", storageKey);
        return;
    }
};

// temporary solution.
// TODO: replace all occurrences of this with reads from SessionContext
export function getStoredOktaToken(): string | undefined {
    const tokenJsonString = localStorage.getItem(
        GLOBAL_STORAGE_KEYS.OKTA_ACCESS_TOKEN,
    );
    if (!tokenJsonString) {
        return "";
    }
    try {
        const tokenJson = JSON.parse(tokenJsonString);
        return tokenJson?.accessToken?.accessToken;
    } catch (e) {
        console.warn("Error retrieving access token", e);
        return "";
    }
}

// temporary solution to read from stored override if available,
// or from stored membership state
// TODO: replace all occurrances of this with reads from SessionContext state
export function getStoredOrg(): string | undefined {
    const override = getOrganizationOverride();
    if (override && override.parsedName) {
        return override.parsedName;
    }
    const storageJson = getSessionMembershipState();
    return storageJson?.activeMembership?.parsedName || "";
}

export function getOrganizationOverride(): MembershipSettings | undefined {
    return fetchJsonFromStorage(GLOBAL_STORAGE_KEYS.ORGANIZATION_OVERRIDE);
}

export function storeOrganizationOverride(override: string) {
    localStorage.setItem(GLOBAL_STORAGE_KEYS.ORGANIZATION_OVERRIDE, override);
}

// not sure this is actually necessary. Okta should handle refresh of non-admin related state
export function getSessionMembershipState(): MembershipState | undefined {
    return fetchJsonFromStorage(GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE);
}

export function storeSessionMembershipState(membershipState: string) {
    localStorage.setItem(GLOBAL_STORAGE_KEYS.MEMBERSHIP_STATE, membershipState);
}

export function getSavedFeatureFlags(): string[] {
    const saved =
        window.localStorage.getItem(FEATURE_FLAG_LOCALSTORAGE_KEY) || "";
    if (saved === "") {
        return [];
    }
    return saved.split("\t");
}

export function storeFeatureFlags(flags: string[]) {
    window.localStorage.setItem(
        FEATURE_FLAG_LOCALSTORAGE_KEY,
        flags.join("\t"),
    );
}
