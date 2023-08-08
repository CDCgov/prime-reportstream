import { fireEvent, screen } from "@testing-library/react";
import { rest } from "msw";

import { renderApp } from "../../utils/CustomRenderUtils";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { settingsServer } from "../../__mocks__/SettingsMockServer";
import { ResponseType, TestResponse } from "../../resources/TestResponse";
import config from "../../config";

import { EditSenderSettings } from "./EditSenderSettings";

const mockData: OrgSenderSettingsResource = new TestResponse(
    ResponseType.SENDER_SETTINGS,
).data;
let editJsonAndSaveButton: HTMLElement;
let nameField: HTMLElement;

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

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => {
        return jest.fn();
    },
    useParams: () => {
        return {
            orgname: "abbott",
            sendername: "user1234",
            action: "edit",
        };
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
                `${config.API_ROOT}/settings/organizations/abbott/senders/user1234`,
                (req, res, ctx) => res(ctx.json(mockData)),
            ),
        );
    });
    afterAll(() => settingsServer.close());
    beforeEach(() => {
        renderApp(<EditSenderSettings />);
        nameField = screen.getByTestId("name");
        editJsonAndSaveButton = screen.getByTestId("submit");
    });
    test("toggle allowDuplicates", () => {
        const checkbox = screen.getByTestId("allowDuplicates");
        expect(checkbox).toBeInTheDocument();
        expect(checkbox).not.toBeChecked();
        fireEvent.click(checkbox);
        expect(checkbox).toBeChecked();
    });
    test("should be able to edit keys field", () => {
        const keysField = screen.getByTestId("keys");

        expect(keysField).toBeInTheDocument();

        fireEvent.change(keysField, { target: { value: testKeys } });

        expect(keysField).toHaveValue(testKeys);
    });

    test("should be able to edit processing type field", () => {
        const processingTypeField = screen.getByTestId("processingType");

        expect(processingTypeField).toBeInTheDocument();

        fireEvent.change(processingTypeField, {
            target: { value: testProcessingType },
        });

        expect(processingTypeField).toHaveValue(testProcessingType);
        fireEvent.click(editJsonAndSaveButton);
        fireEvent.click(screen.getByTestId("editCompareCancelButton"));
        fireEvent.click(screen.getByTestId("senderSettingDeleteButton"));
    });

    describe("should validate name", () => {
        const consoleTraceSpy = jest.fn();

        beforeEach(() => {
            jest.spyOn(console, "trace").mockImplementationOnce(
                consoleTraceSpy,
            );
        });

        afterEach(() => {
            jest.resetAllMocks();
        });

        test("should display an error if name value contains a disallowed char", () => {
            fireEvent.change(nameField, {
                target: { value: "a\\nlinefeed" },
            });
            expect(nameField).toHaveValue("a\\nlinefeed");

            fireEvent.click(editJsonAndSaveButton);
            expect(consoleTraceSpy).toHaveBeenCalled();
        });

        test("should not display error if name value is valid", () => {
            fireEvent.change(nameField, {
                target: { value: "test" },
            });
            expect(nameField).toHaveValue("test");

            fireEvent.click(editJsonAndSaveButton);
            expect(consoleTraceSpy).not.toHaveBeenCalled();
        });
    });
});
