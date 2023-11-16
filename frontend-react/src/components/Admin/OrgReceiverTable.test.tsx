import { screen } from "@testing-library/react";

import { settingsServer } from "../../__mocks__/SettingsMockServer";
import { render } from "../../utils/Test/render";

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

vi.mock("rest-hooks", async () => ({
    ...(await vi.importActual<typeof import("rest-hooks")>("rest-hooks")),
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
        render(<OrgReceiverTable orgname={"test"} key={"test"} />);
    }
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());

    test("renders correctly", () => {
        setup();
        expect(screen.getByText("HL7_BATCH")).toBeInTheDocument();
    });
});
