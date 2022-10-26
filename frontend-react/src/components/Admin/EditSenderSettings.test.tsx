import { fireEvent, screen } from "@testing-library/react";

import { render } from "../../utils/CustomRenderUtils";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { settingsServer } from "../../__mocks__/SettingsMockServer";
import { ResponseType, TestResponse } from "../../resources/TestResponse";

import { EditSenderSettings } from "./EditSenderSettings";

const mockData: OrgSenderSettingsResource = new TestResponse(
    ResponseType.SENDER_SETTINGS
).data;

jest.mock("rest-hooks", () => ({
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
    useNavigate: () => {
        return jest.fn();
    },
    useParams: () => {
        return {
            orgName: "abbott",
            senderName: "user1234",
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
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());
    beforeEach(() => {
        render(<EditSenderSettings />);
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
        fireEvent.click(screen.getByTestId("submit"));
        fireEvent.click(screen.getByTestId("editCompareCancelButton"));
        fireEvent.click(screen.getByTestId("senderSettingDeleteButton"));
    });
});
