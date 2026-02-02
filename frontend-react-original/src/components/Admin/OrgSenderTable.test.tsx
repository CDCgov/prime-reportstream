import { screen } from "@testing-library/react";

import { OrgSenderTable } from "./OrgSenderTable";
import { settingsServer } from "../../__mockServers__/SettingsMockServer";
import { renderApp } from "../../utils/CustomRenderUtils";

const mockData = [
    {
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
        deidentifiedValue: "",
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
        version: null,
        createdBy: null,
        createdAt: null,
        externalName: "The CSV receiver for Ignore",
        timeZone: null,
        dateTimeFormat: "OFFSET",
    },
    {
        name: "HL7",
        organizationName: "ignore",
        topic: "covid-19",
        customerStatus: "inactive",
        translation: {
            schemaName: "fl/fl-covid-19",
            format: "HL7",
            defaults: {},
            nameFormat: "standard",
            receivingOrganization: null,
            type: "CUSTOM",
        },
        jurisdictionalFilter: ["matches(ordering_facility_county, HL7)"],
        qualityFilter: [],
        routingFilter: [],
        processingModeFilter: [],
        reverseTheQualityFilter: false,
        deidentify: false,
        deidentifiedValue: "",
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
        version: null,
        createdBy: null,
        createdAt: null,
        externalName: "Ignore HL7",
        timeZone: null,
        dateTimeFormat: "OFFSET",
    },
    {
        name: "HL7_BATCH",
        organizationName: "ignore",
        topic: "covid-19",
        customerStatus: "inactive",
        translation: {
            schemaName: "az/az-covid-19-hl7",
            format: "HL7_BATCH",
            defaults: {},
            nameFormat: "standard",
            receivingOrganization: null,
            type: "CUSTOM",
        },
        jurisdictionalFilter: ["matches(ordering_facility_county, HL7_BATCH)"],
        qualityFilter: [],
        routingFilter: [],
        processingModeFilter: [],
        reverseTheQualityFilter: false,
        deidentify: false,
        deidentifiedValue: "",
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
        version: null,
        createdBy: null,
        createdAt: null,
        externalName: "Ignore HL7_BATCH",
        timeZone: null,
        dateTimeFormat: "OFFSET",
    },
];

vi.mock("rest-hooks", async (importActual) => ({
    ...(await importActual<typeof import("rest-hooks")>()),
    useResource: () => {
        return mockData;
    },
    useController: () => {
        // fetch is destructured as fetchController in component
        return { fetch: () => mockData };
    },
    // Must return children when mocking, otherwise nothing inside renders
    NetworkErrorBoundary: ({ children }: { children: JSX.Element[] }) => {
        return <>{children}</>;
    },
}));

describe("OrgReceiverTable", () => {
    function setup() {
        renderApp(<OrgSenderTable orgname={"test"} key={"test"} />);
    }
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());

    test("renders correctly", () => {
        setup();
        expect(screen.getByText("HL7_BATCH")).toBeInTheDocument();
    });
});
