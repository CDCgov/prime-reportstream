import { AccessToken } from "@okta/okta-auth-js";

const mockToken = (mock?: Partial<AccessToken>): AccessToken => {
    return {
        authorizeUrl: mock?.authorizeUrl || "",
        expiresAt: mock?.expiresAt || 0,
        scopes: mock?.scopes || [],
        userinfoUrl: mock?.userinfoUrl || "",
        accessToken: mock?.accessToken || "",
        claims: mock?.claims || { sub: "" },
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
