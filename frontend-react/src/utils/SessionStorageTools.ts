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
