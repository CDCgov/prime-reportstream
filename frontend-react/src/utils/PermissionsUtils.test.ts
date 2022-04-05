import { permissionCheck, PERMISSIONS } from "./PermissionsUtils";
import { RSUserClaims } from "./OrganizationUtils";
import { mockToken } from "./TestUtils";

const senderToken = mockToken({
    claims: {
        organization: ["DHSender_ignore"],
    } as RSUserClaims,
});
const receiverToken = mockToken({
    claims: {
        organization: ["DHxx_phd"],
    } as RSUserClaims,
});
const adminToken = mockToken({
    claims: {
        organization: ["DHPrimeAdmins"],
    } as RSUserClaims,
});

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
