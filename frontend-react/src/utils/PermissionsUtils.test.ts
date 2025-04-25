import { AccessTokenWithRSClaims } from "./OrganizationUtils";
import { getUserPermissions, permissionCheck } from "./PermissionsUtils";
import { mockAccessToken } from "./TestUtils";
import { PERMISSIONS } from "./UsefulTypes";

const senderToken = mockAccessToken({
    claims: {
        organization: ["DHSender_ignore"],
    },
} as AccessTokenWithRSClaims);
const receiverToken = mockAccessToken({
    claims: {
        organization: ["DHxx_phd"],
    },
} as AccessTokenWithRSClaims);
const adminToken = mockAccessToken({
    claims: {
        organization: ["DHPrimeAdmins"],
    },
} as AccessTokenWithRSClaims);

describe("permissionCheck", () => {
    test("permission check is valid", () => {
        const trueSenderAuth = permissionCheck(PERMISSIONS.SENDER, senderToken);
        const trueReceiverAuth = permissionCheck(PERMISSIONS.RECEIVER, receiverToken);
        const trueAdminAuth = permissionCheck(PERMISSIONS.PRIME_ADMIN, adminToken);

        const falseSenderAuth = permissionCheck(PERMISSIONS.SENDER, adminToken);
        const falseReceiverAuth = permissionCheck(PERMISSIONS.RECEIVER, senderToken);
        const falseAdminAuth = permissionCheck(PERMISSIONS.PRIME_ADMIN, receiverToken);

        expect(trueSenderAuth).toEqual(true);
        expect(trueReceiverAuth).toEqual(true);
        expect(trueAdminAuth).toEqual(true);

        expect(falseSenderAuth).toEqual(false);
        expect(falseReceiverAuth).toEqual(false);
        expect(falseAdminAuth).toEqual(false);
    });
});

describe("getUserPermissions", () => {
    test("permission groups detected", () => {
        const user = {
            organization: ["DHSender_ignore", "DHxx_phd", "DHPrimeAdmins"],
        } as any;

        const helpers = getUserPermissions(user);

        expect(helpers.isUserAdmin).toBeTruthy();
        expect(helpers.isUserReceiver).toBeTruthy();
        expect(helpers.isUserSender).toBeTruthy();
    });
});
