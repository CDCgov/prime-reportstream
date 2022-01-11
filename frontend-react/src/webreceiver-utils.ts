import { AuthState } from "@okta/okta-auth-js";

import { PERMISSIONS } from "./resources/PermissionsResource";

const groupToOrg = (group: String | undefined): string => {
    // in order to replace all instances of the underscore we needed to use a
    // global regex instead of a string. a string pattern only replaces the first
    // instance
    const isSender = group?.startsWith(PERMISSIONS.SENDER);
    const re = /_/g;
    return group
        ? group.toUpperCase().startsWith("DH")
            ? isSender
                ? group.replace(`${PERMISSIONS.SENDER}_`, "")
                : group.slice(2).replace(re, "-")
            : group.replace(re, "-")
        : "";
};

const getOrganization = (authState: AuthState | null) => {
    return groupToOrg(
        authState!.accessToken?.claims.organization.find(
            (o: string) => !o.toLowerCase().includes("sender")
        )
    );
};

const permissionCheck = (permission: string, authState: AuthState) => {
    if (permission === PERMISSIONS.RECEIVER) {
        return reportReceiver(authState);
    }
    return authState.accessToken?.claims.organization.find((o: string) =>
        o.includes(permission)
    );
};

// A receiver is anyone with an organization that is not "DHSender", i.e.: "DHaz_phd"
const reportReceiver = (authState: AuthState) => {
    return authState.accessToken?.claims.organization.find(
        (o: string | PERMISSIONS[]) => !o.includes(PERMISSIONS.SENDER)
    );
};

const senderClient = (authState: AuthState | null) => {
    if (authState) {
        const claimsSenderOrganization =
            authState?.accessToken?.claims.organization.find(
                (o: string | string[]) => o.includes("DHSender")
            ) || "";
        const claimsSenderOrganizationArray =
            claimsSenderOrganization.split(".");

        // should end up like "DHignore" from "DHSender_ignore.ignore-waters" from Okta
        const claimsOrganization = claimsSenderOrganizationArray[0].replace(
            "Sender_",
            ""
        );

        // should end up like "ignore" from "DHSender_ignore.ignore-waters" from Okta"
        const organizationName = groupToOrg(claimsOrganization);

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

export {
    groupToOrg,
    getOrganization,
    permissionCheck,
    reportReceiver,
    senderClient,
};
