import { act, screen, waitFor } from "@testing-library/react";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import {
    setStoredSenderName,
    setStoredOrg,
} from "../contexts/SessionStorageTools";
import { renderWithSession } from "../utils/CustomRenderUtils";

import SenderModeBanner from "./SenderModeBanner";

jest.mock("@okta/okta-react", () => ({
    useOktaAuth: () => {
        const authState = {
            isAuthenticated: true,
        };
        return { authState: authState };
    },
}));

describe("SenderModeBanner", () => {
    beforeAll(() => {
        orgServer.listen();
        setStoredOrg("testOrg");
        setStoredSenderName("testSender");
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("renders when sender is testing", async () => {
        act(() => {
            renderWithSession(<SenderModeBanner />);
        });
        await waitFor(() => {
            expect(
                screen.getByText("User is in testing mode")
            ).toBeInTheDocument();
        });
    });
});
