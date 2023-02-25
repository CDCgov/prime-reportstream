import { screen, waitFor } from "@testing-library/react";
import { rest } from "msw";
import { Fixture } from "@rest-hooks/test";
import userEvent from "@testing-library/user-event/";

import { renderApp } from "../../utils/CustomRenderUtils";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { settingsServer } from "../../__mocks__/SettingsMockServer";
import { ResponseType, TestResponse } from "../../resources/TestResponse";
import config from "../../config";
import { conditionallySuppressConsole } from "../../utils/TestUtils";

import { EditSenderSettings } from "./EditSenderSettings";

const mockData: OrgSenderSettingsResource = new TestResponse(
    ResponseType.SENDER_SETTINGS
).data;
let editJsonAndSaveButton: HTMLElement;
let nameField: HTMLElement;

const fixtures: Fixture[] = [
    {
        endpoint: OrgSenderSettingsResource.list(),
        args: [{ orgname: mockData.organizationName }],
        error: false,
        response: [mockData],
    },
    {
        endpoint: OrgSenderSettingsResource.detail(),
        args: [
            { orgname: mockData.organizationName, sendername: mockData.name },
        ],
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

const testKeys = JSON.stringify([
    {
        keys: [
            {
                x: "asdfasdasdfasdfasdasdfasdf",
                y: "asdfasdfasdfasdfasdasdfasdfasdasdfasdf",
                crv: "P-384",
                kid: "hca.default",
                kty: "EC",
            },
            {
                e: "AQAB",
                n: "asdfaasdfffffffffffffffffffffffffasdfasdfasdfasdf",
                kid: "hca.default",
                kty: "RSA",
            },
        ],
        scope: "hca.default.report",
    },
]);

const testProcessingType = "sync";

describe("EditSenderSettings", () => {
    beforeAll(() => {
        settingsServer.listen();
        settingsServer.use(
            rest.get(
                `${config.API_ROOT}/settings/organizations/${mockData.organizationName}/senders/${mockData.name}`,
                (req, res, ctx) => res(ctx.json(mockData))
            )
        );
    });
    afterAll(() => settingsServer.close());
    test("toggle allowDuplicates", async () => {
        renderApp(<EditSenderSettings />, {
            restHookFixtures: fixtures,
            initialRouteEntries: [
                `/admin/orgsendersettings/org/${mockData.organizationName}/sender/${mockData.name}/action/edit`,
            ],
        });
        nameField = await screen.findByTestId("name");
        editJsonAndSaveButton = screen.getByTestId("submit");
        const checkbox = screen.getByTestId("allowDuplicates");
        expect(checkbox).toBeInTheDocument();
        expect(checkbox).not.toBeChecked();
        await userEvent.click(checkbox);
        expect(checkbox).toBeChecked();
    });
    test("should be able to edit keys field", async () => {
        renderApp(<EditSenderSettings />, {
            restHookFixtures: fixtures,
            initialRouteEntries: [
                `/admin/orgsendersettings/org/${mockData.organizationName}/sender/${mockData.name}/action/edit`,
            ],
        });
        nameField = await screen.findByTestId("name");
        editJsonAndSaveButton = screen.getByTestId("submit");
        const keysField = screen.getByTestId("keys");

        expect(keysField).toBeInTheDocument();

        await userEvent.clear(keysField);
        await userEvent.type(
            keysField,
            testKeys.replaceAll("[", "[[").replaceAll("{", "{{")
        );

        expect(keysField).toHaveValue(testKeys);
    });

    // DO NOT USE THIS TEST AS AN EXAMPLE. Checking the classList
    // instead of something queryable using testing-library
    // is incorrect. This test implementation is expected
    // to be replaced with a future component refactor.
    test("should be able to edit processing type field", async () => {
        renderApp(<EditSenderSettings />, {
            restHookFixtures: fixtures,
            initialRouteEntries: [
                `/admin/orgsendersettings/org/${mockData.organizationName}/sender/${mockData.name}/action/edit`,
            ],
        });
        nameField = await screen.findByTestId("name");
        editJsonAndSaveButton = screen.getByTestId("submit");
        // This testing implementation is incomplete so silencing
        // an error that occurs
        const restore = conditionallySuppressConsole(
            "Trace: ShowError JSON data"
        );
        const processingTypeField = screen.getByTestId("processingType");
        expect(processingTypeField).toBeInTheDocument();
        const compareModal = screen.getByLabelText(
            "Compare your changes with previous version"
        );

        userEvent.selectOptions(processingTypeField, testProcessingType);
        await waitFor(() =>
            expect(processingTypeField).toHaveValue(testProcessingType)
        );

        userEvent.click(editJsonAndSaveButton);
        await waitFor(() =>
            expect(compareModal.classList).not.toContain("is-hidden")
        );
        userEvent.click(screen.getByTestId("editCompareCancelButton"));
        await waitFor(() =>
            expect(compareModal.classList).toContain("is-hidden")
        );
        userEvent.click(screen.getByTestId("senderSettingDeleteButton"));
        await screen.findByLabelText("Confirm Delete");
        restore();
    });

    describe("should validate name", () => {
        const consoleTraceSpy = jest.fn();

        beforeEach(async () => {
            jest.spyOn(console, "trace").mockImplementationOnce(
                consoleTraceSpy
            );
        });

        afterEach(() => {
            jest.resetAllMocks();
        });

        test("should display an error if name value is prohibited", async () => {
            renderApp(<EditSenderSettings />, {
                restHookFixtures: fixtures,
                initialRouteEntries: [
                    `/admin/orgsendersettings/org/${mockData.organizationName}/sender/${mockData.name}/action/clone`,
                ],
            });
            nameField = await screen.findByTestId("name");
            editJsonAndSaveButton = screen.getByTestId("submit");
            await userEvent.clear(nameField);
            await userEvent.type(nameField, "Organization");
            expect(nameField).toHaveValue("Organization");

            await userEvent.click(editJsonAndSaveButton);
            expect(consoleTraceSpy).toHaveBeenCalled();
        });

        test("should display an error if name value contains a non-alphanumeric char", async () => {
            renderApp(<EditSenderSettings />, {
                restHookFixtures: fixtures,
                initialRouteEntries: [
                    `/admin/orgsendersettings/org/${mockData.organizationName}/sender/${mockData.name}/action/clone`,
                ],
            });
            nameField = await screen.findByTestId("name");
            editJsonAndSaveButton = screen.getByTestId("submit");
            await userEvent.clear(nameField);
            await userEvent.type(nameField, "a\\nlinefeed");
            expect(nameField).toHaveValue("a\\nlinefeed");

            await userEvent.click(editJsonAndSaveButton);
            expect(consoleTraceSpy).toHaveBeenCalled();
        });

        test("should not display error if name value is valid", async () => {
            renderApp(<EditSenderSettings />, {
                restHookFixtures: fixtures,
                initialRouteEntries: [
                    `/admin/orgsendersettings/org/${mockData.organizationName}/sender/${mockData.name}/action/clone`,
                ],
            });
            nameField = await screen.findByTestId("name");
            editJsonAndSaveButton = screen.getByTestId("submit");
            await userEvent.clear(nameField);
            await userEvent.type(nameField, "test");
            expect(nameField).toHaveValue("test");

            userEvent.click(editJsonAndSaveButton);
            await screen.findAllByLabelText(
                "Compare your changes with previous version"
            );
            expect(consoleTraceSpy).not.toHaveBeenCalled();
        });
    });
});
