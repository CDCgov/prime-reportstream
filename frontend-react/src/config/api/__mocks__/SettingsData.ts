export const mockSenderKey: SenderKeys = {
    keys: [
        {
            x: "asdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdf",
            y: "asdfasdfasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdfasdfasdasdf",
            crv: "P-384",
            kid: "hca.default",
            kty: "EC",
        },
        {
            e: "AQAB",
            n: "asdfaasdfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffasdfasdfasdfasdf",
            kid: "hca.default",
            kty: "RSA",
        },
    ],
    scope: "hca.default.report",
};

export const mockSenderSettingsPUTResponse: Partial<RSSender> = {
    keys: [mockSenderKey],
    topic: "covid-19",
    format: "HL7",
    schemaName: "direct/hca-covid-19",
    customerStatus: "active",
    processingType: "sync",
    organizationName: "hca",
    name: "",
    version: 0,
    createdBy: "mctest@example.com",
    createdAt: "1/1/2000 00:00:00",
};

export const mockOrganizationSettingsPOSTResponse: Partial<RSOrganizationSettings> =
    {
        name: "test",
        description: "A Test Organization",
        jurisdiction: "STATE",
        countyName: "Test",
        stateCode: "CA",
    };
