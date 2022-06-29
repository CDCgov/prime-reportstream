import { AccessToken } from "@okta/okta-auth-js";

import { RSUserClaims } from "./OrganizationUtils";

const mockToken = (mock?: Partial<AccessToken>) => {
    const mockedClaims: RSUserClaims = mock?.claims
        ? mock.claims
        : {
              sub: "",
              // organization: "",
          };

    return {
        authorizeUrl: mock?.authorizeUrl || "",
        expiresAt: mock?.expiresAt || 0,
        scopes: mock?.scopes || [],
        userinfoUrl: mock?.userinfoUrl || "",
        accessToken: mock?.accessToken || "",
        claims: mockedClaims,
        tokenType: mock?.tokenType || "",
    };
};

export { mockToken };

const mockEvent = (mock?: Partial<any>) => {
    return {
        response: mock?.response || null,
    };
};

export { mockEvent };
