import { AccessToken } from "@okta/okta-auth-js";

import { permissionCheck, PERMISSIONS } from "./PermissionsUtils";
import { RSUserClaims } from "./OrganizationUtils";

const senderToken: AccessToken = {
    authorizeUrl: "",
    expiresAt: 0,
    scopes: [],
    userinfoUrl: "",
    accessToken: "",
    claims: {
        organization: ["DHSender_ignore"],
    } as RSUserClaims,
    tokenType: "",
};

const receiverToken: AccessToken = {
    authorizeUrl: "",
    expiresAt: 0,
    scopes: [],
    userinfoUrl: "",
    accessToken: "",
    claims: {
        organization: ["DHxx_phd"],
    } as RSUserClaims,
    tokenType: "",
};

const adminToken: AccessToken = {
    authorizeUrl: "",
    expiresAt: 0,
    scopes: [],
    userinfoUrl: "",
    accessToken: "",
    claims: {
        organization: ["DHPrimeAdmins"],
    } as RSUserClaims,
    tokenType: "",
};

test("permissionCheck", () => {
    const trueSenderAuth = permissionCheck(PERMISSIONS.SENDER, senderToken);
    const trueReceiverAuth = permissionCheck(
        PERMISSIONS.RECEIVER,
        receiverToken
    );
    const trueAdminAuth = permissionCheck(PERMISSIONS.PRIME_ADMIN, adminToken);

    const falseSenderAuth = permissionCheck(PERMISSIONS.SENDER, adminToken);
    const falseReceiverAuth = permissionCheck(
        PERMISSIONS.RECEIVER,
        senderToken
    );
    const falseAdminAuth = permissionCheck(
        PERMISSIONS.PRIME_ADMIN,
        receiverToken
    );

    expect(trueSenderAuth).toEqual(true);
    expect(trueReceiverAuth).toEqual(true);
    expect(trueAdminAuth).toEqual(true);

    expect(falseSenderAuth).toEqual(false);
    expect(falseReceiverAuth).toEqual(false);
    expect(falseAdminAuth).toEqual(false);
});
