import { AccessToken, CustomUserClaims, UserClaims } from "@okta/okta-auth-js";

import { PERMISSIONS } from "./PermissionsUtils";

enum RSOrgType {
    SENDER = "sender",
    RECEIVER = "receiver",
    ADMIN = "admin",
}

/* New claims not present in UserClaims need tobe added via
 * this interface so UserClaims can implement the fields. */
interface RSExtraClaims extends CustomUserClaims {
    organization: string[];
}
type RSUserClaims = UserClaims<RSExtraClaims>;
export const toRSClaims = (claims: UserClaims): RSUserClaims => {
    return claims as RSUserClaims;
};

export interface AccessTokenWithRSClaims extends AccessToken {
    claims: RSUserClaims;
}

/* Parses the array of organizations (strings) from an AccessToken */
const getOktaGroups = (accessToken: AccessToken | undefined): string[] => {
    if (!accessToken?.claims) return [];
    return toRSClaims(accessToken.claims).organization || [];
};

/* Converts Okta group names to their respective organization name
 * counterparts that we store in the RS database */
const parseOrgName = (group: string | undefined): string => {
    const senderPrefix = `${PERMISSIONS.SENDER}_`;
    const isStandardGroup = group?.startsWith("DH");
    const isSenderGroup = group?.startsWith(senderPrefix);
    /* Quick definitions:
     * Sender - has an OktaGroup name beginning with DHSender_
     * Receiver - has an OktaGroup name beginning with DHxx_ where xx is any state code
     * Non-standard - has no associated OktaGroup and is already the org value in the db table */
    const groupType = isStandardGroup
        ? isSenderGroup
            ? "sender"
            : "receiver"
        : "non-standard";

    switch (groupType) {
        case "sender":
            // DHSender_test_sender -> test_sender
            // DHSender_another-test-sender -> another-test-sender
            return group ? group.replace(senderPrefix, "") : "";
        case "receiver":
            // DHmd_phd -> md-phd
            return group ? group.replace("DH", "").replace(/_/g, "-") : "";
        case "non-standard":
            // simple_report -> simple_report
            return group ? group : "";
    }
};

export { RSOrgType, getOktaGroups, parseOrgName };
