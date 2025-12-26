import { MembershipSettings } from "./OrganizationUtils";
import { RSSender } from "../config/endpoints/settings";

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

export function getSavedFeatureFlags(): string[] {
    const saved = window.localStorage.getItem(FEATURE_FLAG_LOCALSTORAGE_KEY) ?? "";
    if (saved === "") {
        return [];
    }
    return saved.split("\t");
}

export function storeFeatureFlags(flags: string[]) {
    window.localStorage.setItem(FEATURE_FLAG_LOCALSTORAGE_KEY, flags.join("\t"));
}

/**
 * Given a user's membership settings and their Sender details,
 * return the client string to send to the validate endpoint
 *
 * Only send the client when the selected schema matches the Sender's schema --
 * this is to account for factoring in Sender settings into the validation
 * e.g., allowDuplicates in https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/src/main/kotlin/azure/ValidateFunction.kt#L100
 *
 * @param selectedSchemaName { string | undefined }
 * @param activeMembership { MembershipSettings | undefined}
 * @param sender { RSSender | undefined }
 * @returns {string} The value sent as the client header (can be a blank string)
 */
export function getClientHeader(
    selectedSchemaName: string | undefined,
    activeMembership: MembershipSettings | null | undefined,
    sender: RSSender | undefined,
) {
    const parsedName = activeMembership?.parsedName;
    const senderName = activeMembership?.service;

    if (parsedName && senderName && sender?.schemaName === selectedSchemaName) {
        return `${parsedName}.${senderName}`;
    }

    return "";
}
