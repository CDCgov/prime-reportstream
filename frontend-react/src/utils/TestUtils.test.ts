import { AccessToken } from "@okta/okta-auth-js";

import { mockToken } from "./TestUtils";
import { AccessTokenWithRSClaims } from "./OrganizationUtils";

test("mockToken", () => {
    const emptyAccessToken: AccessToken = mockToken(); // Returns all default values
    const mockedAccessToken: AccessToken = mockToken({
        claims: {
            organization: ["DHSender_xx-phd", "DHxx_phd"],
        },
    } as AccessTokenWithRSClaims);

    expect(emptyAccessToken).toEqual({
        authorizeUrl: "",
        expiresAt: 0,
        scopes: [],
        userinfoUrl: "",
        accessToken: "",
        claims: {
            sub: "", // This satisfies the required attribute
        },
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
        },
        tokenType: "",
    });
});
