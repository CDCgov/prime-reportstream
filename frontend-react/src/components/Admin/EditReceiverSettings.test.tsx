import { fireEvent, screen } from "@testing-library/react";

import { renderWithQueryProvider } from "../../utils/CustomRenderUtils";
import { settingsServer } from "../../config/api/__mocks__/SettingsMockServer";
import { mockOrganizationReceiverSettings } from "../../config/api/__mocks__/SettingsData";

import { EditReceiverSettings } from "./EditReceiverSettings";

jest.mock("../../hooks/api/settings/UseOrganizationReceiverSettings", () => ({
    useOrganizationReceiverSettings: () => ({
        data: mockOrganizationReceiverSettings,
    }),
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

describe("EditReceiverSettings", () => {
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());
    beforeEach(() => {
        renderWithQueryProvider(<EditReceiverSettings />);
    });

    test("should be able to edit keys field", () => {
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
