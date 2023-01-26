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

export const mockOrganizationReceiverSettings: Partial<RSReceiver> = {
    name: "CSV",
    organizationName: "ignore",
    topic: "covid-19",
    customerStatus: "inactive",
    translation: {
        schemaName: "az/pima-az-covid-19",
        format: "CSV",
        defaults: {},
        nameFormat: "standard",
        receivingOrganization: null,
        type: "CUSTOM",
    },
    jurisdictionalFilter: ["matches(ordering_facility_county, CSV)"],
    qualityFilter: [],
    routingFilter: [],
    processingModeFilter: [],
    reverseTheQualityFilter: false,
    deidentify: false,
    //deidentifiedValue: "",
    timing: {
        operation: "MERGE",
        numberPerDay: 1440,
        initialTime: "00:00",
        timeZone: "EASTERN",
        maxReportCount: 100,
        whenEmpty: {
            action: "NONE",
            onlyOncePerDay: false,
        },
    },
    description: "",
    transport: {
        host: "sftp",
        port: "22",
        filePath: "./upload",
        credentialName: "DEFAULT-SFTP",
        type: "SFTP",
    },
    version: 0,
    createdBy: "local@test.com",
    createdAt: "2022-05-25T15:36:27.589Z",
    externalName: "The CSV receiver for Ignore",
    //timeZone: null,
    //dateTimeFormat: "OFFSET",
};
