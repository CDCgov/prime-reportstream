import { screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { http, HttpResponse } from "msw";

import { useResource } from "rest-hooks";
import { EditSenderSettingsPage } from "./EditSenderSettings";
import { settingsServer } from "../../__mockServers__/SettingsMockServer";
import config from "../../config";
import { useToast } from "../../contexts/Toast";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { ResponseType, TestResponse } from "../../resources/TestResponse";
import { renderApp } from "../../utils/CustomRenderUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../contexts/Session/__mocks__/useSessionContext")
>("../../contexts/Session/useSessionContext");

const mockData: OrgSenderSettingsResource = new TestResponse(ResponseType.SENDER_SETTINGS).data;
let editJsonAndSaveButton: HTMLElement;
const mockUseToast = vi.mocked(useToast);
const mockCtx = mockUseToast();
const mockUseResource = vi.mocked(useResource);

vi.mock("rest-hooks", async (importActual) => ({
    ...(await importActual<typeof import("rest-hooks")>()),
    useResource: vi.fn(),
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
            sendername: "user1234",
            action: "edit",
        };
    },
}));

vi.mock("../../contexts/Toast");

describe("EditSenderSettings", () => {
    async function setup(data: Partial<OrgSenderSettingsResource> = mockData) {
        if (data) mockUseResource.mockReturnValue(data as any);
        renderApp(<EditSenderSettingsPage />);
        editJsonAndSaveButton = screen.getByRole("button", {
            name: "Edit json and save...",
        });
        await waitFor(() => expect(editJsonAndSaveButton).toBeEnabled());
    }
    beforeAll(() => {
        mockSessionContentReturnValue();
        settingsServer.listen();
        settingsServer.use(
            http.get(`${config.API_ROOT}/settings/organizations/abbott/senders/user1234`, () =>
                HttpResponse.json(mockData),
            ),
        );
    });
    afterAll(() => settingsServer.close());

    describe("should validate name", () => {
        test("name field disabled", async () => {
            await setup();
            expect(screen.getByLabelText("Name:", { exact: true })).toBeDisabled();
        });
        describe("on Edit json and save", () => {
            async function editSetup(data?: Partial<OrgSenderSettingsResource>) {
                await setup(data);
                await userEvent.click(editJsonAndSaveButton);
            }
            afterEach(() => void vi.clearAllMocks());
            test("should display an error if name value contains a disallowed char", async () => {
                await editSetup({ ...mockData, name: "a\\nlinefeed" });
                await waitFor(() => expect(mockCtx.toast).toHaveBeenCalled());
            });
            test("should not display error if name value is valid", async () => {
                await editSetup();
                await waitFor(() => expect(mockCtx.toast).not.toHaveBeenCalled());
            });
        });
    });
});
