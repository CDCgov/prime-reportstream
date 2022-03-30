import { AccessToken } from "@okta/okta-auth-js";

import { RSUserClaims } from "./OrganizationUtils";

const mockToken = (mock?: Partial<AccessToken>) => {
    return {
        authorizeUrl: mock?.authorizeUrl || "",
        expiresAt: mock?.expiresAt || 0,
        scopes: mock?.scopes || [],
        userinfoUrl: mock?.userinfoUrl || "",
        accessToken: mock?.accessToken || "",
        claims:
            mock?.claims ||
            ({
                sub: "",
            } as RSUserClaims),
        tokenType: mock?.tokenType || "",
    };
};

export { mockToken };
