import { screen } from "@testing-library/react";

import { OrgReceiverTable } from "./OrgReceiverTable";
import { settingsServer } from "../../__mocks__/SettingsMockServer";
import { renderApp } from "../../utils/CustomRenderUtils";

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

jest.mock("rest-hooks", () => ({
    ...jest.requireActual("rest-hooks"),
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
        renderApp(<OrgReceiverTable orgname={"test"} key={"test"} />);
    }
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());

    test("renders correctly", () => {
        setup();
        expect(screen.getByText("HL7_BATCH")).toBeInTheDocument();
    });
});
