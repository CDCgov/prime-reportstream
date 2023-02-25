import { Fixture } from "@rest-hooks/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { rest } from "msw";

import config from "../../config";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { renderApp } from "../../utils/CustomRenderUtils";
import { conditionallySuppressConsole } from "../../utils/TestUtils";
import { settingsServer } from "../../__mocks__/SettingsMockServer";

import { EditReceiverSettings } from "./EditReceiverSettings";

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

const fixtures: Fixture[] = [
    {
        endpoint: OrgReceiverSettingsResource.list(),
        args: [{ orgname: "abbott" }],
        error: false,
        response: [mockData],
    },
    {
        endpoint: OrgReceiverSettingsResource.detail(),
        args: [{ orgname: "abbott", receivername: "user1234", action: "edit" }],
        error: false,
        response: mockData,
    },
];

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => {
        return jest.fn();
    },
}));

describe("EditReceiverSettings", () => {
    beforeAll(() => {
        settingsServer.listen();
        settingsServer.use(
            rest.get(
                `${config.API_ROOT}/settings/organizations/abbott/receivers/user1234`,
                (req, res, ctx) => res(ctx.json(mockData))
            )
        );
    });
    afterAll(() => settingsServer.close());
    beforeEach(() => {
        renderApp(<EditReceiverSettings />, {
            restHookFixtures: fixtures,
            initialRouteEntries: [
                "/admin/orgreceiversettings/org/abbott/receiver/user1234/action/edit",
            ],
        });
    });

    // DO NOT USE THIS TEST AS AN EXAMPLE. Checking the classList
    // instead of something queryable using testing-library
    // is incorrect. This test implementation is expected
    // to be replaced with a future component refactor.
    test("should be able to edit keys field", async () => {
        // This testing implementation is incomplete so silencing
        // an error that occurs
        const restore = conditionallySuppressConsole(
            "Trace: ShowError JSON data"
        );
        const descriptionField = await screen.findByTestId<HTMLInputElement>(
            "description"
        );
        expect(descriptionField).toBeInTheDocument();
        const compareModal = screen.getByLabelText(
            "Compare your changes with previous version"
        );

        userEvent.type(descriptionField, "Testing Edit");
        await waitFor(() =>
            expect(descriptionField).toHaveValue("Testing Edit")
        );
        userEvent.click(screen.getByTestId("submit"));
        await waitFor(() =>
            expect(compareModal.classList).not.toContain("is-hidden")
        );
        userEvent.click(screen.getByTestId("editCompareCancelButton"));
        await waitFor(() =>
            expect(compareModal.classList).toContain("is-hidden")
        );
        userEvent.click(screen.getByTestId("receiverSettingDeleteButton"));
        await screen.findByLabelText("Confirm Delete");
        restore();
    });
});
