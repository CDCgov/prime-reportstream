import { Fixture } from "@rest-hooks/test";
import { screen } from "@testing-library/react";
import { Suspense } from "react";

import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { renderApp } from "../../utils/CustomRenderUtils";
import { checkSettingsServer } from "../../__mocks__/CheckSettingsMockServer";
import { settingsServer } from "../../__mocks__/SettingsMockServer";
import Spinner from "../Spinner";

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
        args: [{ orgname: "test" }],
        error: false,
        response: mockData,
    },
];

describe("OrgReceiverTable", () => {
    beforeAll(() => {
        settingsServer.listen();
        checkSettingsServer.listen();
    });
    afterEach(() => {
        settingsServer.resetHandlers();
        checkSettingsServer.resetHandlers();
    });
    afterAll(() => {
        settingsServer.close();
        checkSettingsServer.close();
    });
    beforeEach(() => {
        renderApp(
            <Suspense fallback={<Spinner />}>
                <OrgReceiverTable orgname={"test"} key={"test"} />
            </Suspense>,
            {
                restHookFixtures: fixtures,
            }
        );
    });

    test("renders correctly", async () => {
        await screen.findByText("HL7_BATCH");
    });
});
