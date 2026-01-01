import { fireEvent, screen } from "@testing-library/react";

import { AdminOrgNewPage } from "./AdminOrgNew";
import { settingsServer } from "../../__mockServers__/SettingsMockServer";
import OrganizationResource from "../../resources/OrganizationResource";
import { ResponseType, TestResponse } from "../../resources/TestResponse";
import { renderApp } from "../../utils/CustomRenderUtils";

const mockData: OrganizationResource = new TestResponse(
    ResponseType.NEW_ORGANIZATION,
).data;

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
    __esModule: true,
    useNavigate: () => vi.fn(),
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
    function setup() {
        renderApp(<AdminOrgNewPage />);
    }

    test("should go to the new created organization's page", () => {
        setup();
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
