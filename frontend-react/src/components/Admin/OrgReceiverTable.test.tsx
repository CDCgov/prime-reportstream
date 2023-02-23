import { Fixture } from "@rest-hooks/test";
import { screen } from "@testing-library/react";

import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { renderApp } from "../../utils/CustomRenderUtils";
import { settingsServer } from "../../__mocks__/SettingsMockServer";

import { OrgReceiverTable } from "./OrgReceiverTable";

const mockData = [
    {
        name: "CSV",
        organizationName: "ignore",
        topic: "covid-19",
        customerStatus: "inactive",
        translation: {},
        jurisdictionalFilter: [],
        qualityFilter: [],
        routingFilter: [],
        processingModeFilter: [],
        reverseTheQualityFilter: false,
        deidentify: false,
        deidentifiedValue: "",
        timing: {},
        description: "",
        transport: {},
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
        translation: {},
        jurisdictionalFilter: [],
        qualityFilter: [],
        routingFilter: [],
        processingModeFilter: [],
        reverseTheQualityFilter: false,
        deidentify: false,
        deidentifiedValue: "",
        timing: {},
        description: "",
        transport: {},
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
        translation: {},
        jurisdictionalFilter: [],
        qualityFilter: [],
        routingFilter: [],
        processingModeFilter: [],
        reverseTheQualityFilter: false,
        deidentify: false,
        deidentifiedValue: "",
        timing: {},
        description: "",
        transport: {},
        version: null,
        createdBy: null,
        createdAt: null,
        externalName: "Ignore HL7_BATCH",
        timeZone: null,
        dateTimeFormat: "OFFSET",
    },
];

const fixtures: Fixture[] = [
    {
        endpoint: OrgReceiverSettingsResource.list(),
        args: [{ orgname: "ignore" }],
        error: false,
        response: mockData,
    },
];

describe("OrgReceiverTable", () => {
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());
    beforeEach(() => {
        renderApp(<OrgReceiverTable orgname={"test"} key={"test"} />, {
            restHookFixtures: fixtures,
        });
    });

    test("renders correctly", () => {
        expect(screen.getByText("HL7_BATCH")).toBeInTheDocument();
    });
});
