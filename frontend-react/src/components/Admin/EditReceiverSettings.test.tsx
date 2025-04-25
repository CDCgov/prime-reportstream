import { fireEvent, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";

import { EditReceiverSettingsPage } from "./EditReceiverSettings";
import { settingsServer } from "../../__mockServers__/SettingsMockServer";
import config from "../../config";
import { renderApp } from "../../utils/CustomRenderUtils";

const mockData = {
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
    version: 0,
    createdBy: "local@test.com",
    createdAt: "2022-05-25T15:36:27.589Z",
    externalName: "The CSV receiver for Ignore",
    timeZone: null,
    dateTimeFormat: "OFFSET",
};

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

vi.mock("react-router-dom", async (importActual) => ({
    ...(await importActual<typeof import("react-router-dom")>()),
    useNavigate: () => {
        return vi.fn();
    },
    useParams: () => {
        return {
            orgname: "abbott",
            receivername: "user1234",
            action: "edit",
        };
    },
}));

describe("EditReceiverSettings", () => {
    function setup() {
        renderApp(<EditReceiverSettingsPage />);
    }

    beforeAll(() => {
        global.window = Object.create(window);
        const url = "http://test.com";
        Object.defineProperty(window, "location", {
            value: {
                href: url,
            },
            writable: true,
        });
        settingsServer.listen();
        settingsServer.use(
            http.get(`${config.API_ROOT}/settings/organizations/abbott/receivers/user1234`, () =>
                HttpResponse.json(mockData),
            ),
        );
    });
    afterAll(() => {
        setup();
        settingsServer.close();
    });

    test("should be able to edit keys field", () => {
        setup();
        const descriptionField = screen.getByTestId("description");
        expect(descriptionField).toBeInTheDocument();

        fireEvent.change(descriptionField, {
            target: { value: "Testing Edit" },
        });

        expect(descriptionField).toHaveValue("Testing Edit");
        fireEvent.click(screen.getByTestId("submit"));
        fireEvent.click(screen.getByTestId("editCompareCancelButton"));
        fireEvent.click(screen.getByTestId("receiverSettingDeleteButton"));
    });
});
