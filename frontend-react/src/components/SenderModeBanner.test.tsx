import { screen } from "@testing-library/react";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import { renderApp } from "../utils/CustomRenderUtils";
import { mockSessionContentReturnValue } from "../contexts/__mocks__/SessionContext";
import { MemberType } from "../hooks/UseOktaMemberships";

import SenderModeBanner from "./SenderModeBanner";

describe("SenderModeBanner", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("renders when sender is testing", async () => {
        mockSessionContentReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testSender",
            },

            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
            environment: "test",
        });
        renderApp(<SenderModeBanner />);
        const text = await screen.findByText("Learn more about onboarding.");
        expect(text).toBeInTheDocument();
    });
});
