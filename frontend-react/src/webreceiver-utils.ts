import { AccessToken, AuthState, UserClaims } from "@okta/okta-auth-js";

import { PERMISSIONS } from "./resources/PermissionsResource";
import { getStoredOrg } from "./contexts/SessionStorageTools";
import { groupToOrg } from "./utils/OrganizationUtils";

declare type RSUserClaims = UserClaims<{ organization: string[] }>;

export const getOrganizationFromAccessToken = (
    accessToken: AccessToken | undefined
): string[] => {
    const newclaim: RSUserClaims =
        (accessToken?.claims as RSUserClaims) || undefined;
    return newclaim?.organization || [];
};

const getOrganization = (authState: AuthState | undefined | null) => {
    return groupToOrg(
        getOrganizationFromAccessToken(authState?.accessToken).find(
            (o: string) => !o.toLowerCase().includes("sender")
        )
    );
};

const permissionCheck = (
    permission: string,
    authState: AuthState | undefined | null
) => {
    if (permission === PERMISSIONS.RECEIVER) {
        return reportReceiver(authState || undefined);
    }

    /* Right now, we're checking for a "DHSender" substring, not an exact match */
    return getOrganizationFromAccessToken(authState?.accessToken).find(
        (org: string) => org.includes(permission)
    );
};

// A receiver is anyone with an organization that is not "DHSender", i.e.: "DHaz_phd"
const reportReceiver = (authState: AuthState | undefined | null) => {
    return getOrganizationFromAccessToken(authState?.accessToken).find(
        (o: string | PERMISSIONS[]) => !o.includes(PERMISSIONS.SENDER)
    );
};

const senderClient = (authState: AuthState | undefined | null) => {
    if (authState) {
        const claimsSenderOrganization =
            getOrganizationFromAccessToken(authState?.accessToken).find(
                (o: string | string[]) => o.includes("DHSender")
            ) || "";
        const claimsSenderOrganizationArray =
            claimsSenderOrganization.split(".");

        // should end up like "ignore" from "DHSender_ignore.ignore-waters" from Okta"
        const organizationName = getStoredOrg();

        // should end up like "ignore.ignore_waters" from "DHSender_ignore.ignore-waters" from Okta.
        // This is used on the RS side to validate the user claims, so, it need the underscores ("_")
        // The ternary checks if there is anything after the "." in the group name, if not, it leaves it blank
        // i.e. if the sender name is "DHSender_all-in-one-health-ca", there will be no "."
        const senderName = claimsSenderOrganizationArray[1]
            ? `.${claimsSenderOrganizationArray[1]}`
            : "";
        return `${organizationName}${senderName}`;
    }
    return "";
};

export { getOrganization, permissionCheck, reportReceiver, senderClient };
