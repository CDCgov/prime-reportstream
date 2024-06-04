import { screen } from "@testing-library/react";

import SenderModeBanner from "./SenderModeBanner";
import { orgServer } from "../__mockServers__/OrganizationMockServer";
import { renderApp } from "../utils/CustomRenderUtils";
import { MemberType } from "../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../contexts/Session/__mocks__/useSessionContext")
>("../contexts/Session/useSessionContext");

describe("SenderModeBanner", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("renders when sender is testing", async () => {
        mockSessionContentReturnValue({
            authState: {
                accessToken: { accessToken: "TOKEN" },
            } as any,
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testSender",
            },

            user: {
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: true,
                isUserTransceiver: false,
            } as any,
        });
        renderApp(<SenderModeBanner />);
        const text = await screen.findByText("Learn more about onboarding.");
        expect(text).toBeInTheDocument();
    });
});
