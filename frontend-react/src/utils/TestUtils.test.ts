import { AccessToken } from "@okta/okta-auth-js";

import { mockToken } from "./TestUtils";
import { RSUserClaims } from "./OrganizationUtils";

test("mockToken", () => {
    const emptyAccessToken: AccessToken = mockToken(); // Returns all default values
    const mockedAccessToken: AccessToken = mockToken({
        claims: {
            organization: ["DHSender_xx-phd", "DHxx_phd"],
        } as RSUserClaims,
    });

    expect(emptyAccessToken).toEqual({
        authorizeUrl: "",
        expiresAt: 0,
        scopes: [],
        userinfoUrl: "",
        accessToken: "",
        claims: {
            sub: "", // This satisfies the required attribute
        } as RSUserClaims,
        tokenType: "",
    });
    expect(mockedAccessToken).toEqual({
        authorizeUrl: "",
        expiresAt: 0,
        scopes: [],
        userinfoUrl: "",
        accessToken: "",
        claims: {
            organization: ["DHSender_xx-phd", "DHxx_phd"],
        } as RSUserClaims,
        tokenType: "",
    });
});
