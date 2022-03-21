import { act, screen, waitFor } from "@testing-library/react";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import {
    setStoredSenderName,
    setStoredOrg,
} from "../contexts/SessionStorageTools";
import { renderWithRouter } from "../utils/CustomRenderUtils";

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

    /* Test passes, but throws the ol' "wrap it with act()" warning
     * in the console despite, you know, it being wrapped with act().
     * This is related to the hook's effect that fetches data when
     * state changes. */
    test("renders when sender is testing", async () => {
        act(() => {
            renderWithRouter(<SenderModeBanner />);
        });
        await waitFor(() => {
            expect(
                screen.getByText("User is in testing mode")
            ).toBeInTheDocument();
        });
    });
});
