import { ReportHistory } from "config/endpoints/deliveries";
import {
    OrganizationSenderSettings,
    OrganizationSettings,
} from "config/endpoints/settings";

export const mockActionDetails: ReportHistory = {
    submissionId: 12345,
    timestamp: "1970-04-07T16:26:14.345Z",
    sender: "Jest",
    httpStatus: 201,
    externalName: "SubmissionDetails Unit Test",
    id: "x0000xx0-0xx0-0000-0x00-00x000x0x000",
    destinations: [
        {
            organization_id: "jest",
            organization: "React Unit Testing",
            service: "Primary",
            filteredReportRows: [
                "For ignore.Primary, filter matches[ordering_facility_county, Primary] filtered out item 682740 at index 1",
                "For ignore.Primary, filter matches[ordering_facility_county, Primary] filtered out item 496898 at index 3",
            ],
            sending_at: "1970-04-07T16:26:14.345Z",
            itemCount: 3,
            sentReports: [],
            filteredReportItems: [],
            itemCountBeforeQualityFiltering: 0,
        },
    ],
    errors: [
        {
            scope: "",
            errorCode: "",
            type: "",
            message: "",
            index: 0,
            trackingId: "",
        },
    ],
    warnings: [
        {
            scope: "",
            errorCode: "",
            type: "",
            message: "",
        },
    ],
    topic: "TEST",
    warningCount: 0,
    errorCount: 0,
};

export const mockSenderSettingsPutResponse: Partial<OrganizationSenderSettings> =
    {
        keys: [
            {
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
            },
        ],
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

export const mockNewOrgResponse: Partial<OrganizationSettings> = {
    name: "test",
    description: "A Test Organization",
    jurisdiction: "STATE",
    countyName: "Test",
    stateCode: "CA",
};
