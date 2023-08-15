import { fireEvent, screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";
import { settingsServer } from "../../__mocks__/SettingsMockServer";
import { ResponseType, TestResponse } from "../../resources/TestResponse";
import OrganizationResource from "../../resources/OrganizationResource";

import { AdminOrgNew } from "./AdminOrgNew";

const mockData: OrganizationResource = new TestResponse(
    ResponseType.NEW_ORGANIZATION,
).data;

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
    __esModule: true,
    useNavigate: () => jest.fn(),
}));

const testNewOrgJson = JSON.stringify({
    name: "test",
    description: "A Test Organization",
    jurisdiction: "STATE",
    stateCode: "CA",
    countyName: null,
    version: null,
    createdAt: null,
    createdBy: null,
});

describe("AdminOrgNew", () => {
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());
    beforeEach(() => {
        renderApp(<AdminOrgNew />);
    });

    test("should go to the new created organization's page", () => {
        // orgName field
        const orgNameField = screen.getByTestId("orgName");
        expect(orgNameField).toBeInTheDocument();
        fireEvent.change(orgNameField, { target: { value: "test" } });
        expect(orgNameField).toHaveValue("test");

        // orgSetting field
        const orgSettingField = screen.getByTestId("orgSetting");
        expect(orgNameField).toBeInTheDocument();
        fireEvent.change(orgSettingField, {
            target: { value: testNewOrgJson },
        });
        expect(orgSettingField).toHaveValue(testNewOrgJson);

        // submit button
        const submitButton = screen.getByTestId("submit");
        expect(submitButton).toBeInTheDocument();
        fireEvent.click(submitButton);
    });
});
