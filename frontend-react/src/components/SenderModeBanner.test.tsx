import { screen } from "@testing-library/react";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import {
    setStoredSenderName,
    setStoredOrg,
} from "../contexts/SessionStorageTools";
import { makeOktaHook, renderWithSession } from "../utils/CustomRenderUtils";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";
import { MembershipController, MemberType } from "../hooks/UseOktaMemberships";
import { SessionController } from "../hooks/UseSessionStorage";

import SenderModeBanner from "./SenderModeBanner";

describe("SenderModeBanner", () => {
    beforeAll(() => {
        orgServer.listen();
        setStoredOrg("testOrg");
        setStoredSenderName("testSender");
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("renders when sender is testing", async () => {
        mockSessionContext.mockReturnValue({
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.SENDER,
                        parsedName: "testOrg",
                        senderName: "testSender",
                    },
                },
            } as MembershipController,
            store: {} as SessionController, // TS yells about removing this because of types
        });
        renderWithSession(
            <SenderModeBanner />,
            makeOktaHook({
                authState: {
                    isAuthenticated: true,
                },
            })
        );
        const text = await screen.findByText("Learn more about onboarding.");
        expect(text).toBeInTheDocument();
    });
});
