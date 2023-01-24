const MockSenderOrganizationClaims = ["DHSender_xx-phd", "DHxx_phd"] as const; // satisfies readonly CustomUserClaim[]
export const mockSenderAccessToken = {
    authorizeUrl: "",
    expiresAt: 0,
    scopes: [],
    userinfoUrl: "",
    accessToken: "abcd1234",
    claims: {
        organization: MockSenderOrganizationClaims,
        sub: "",
    },
    tokenType: "",
} as const; // satisfies RecursiveReadonly<AccessToken>;
