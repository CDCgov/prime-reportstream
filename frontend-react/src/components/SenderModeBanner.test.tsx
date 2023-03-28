import { screen } from "@testing-library/react";

import { settingsServer } from "../__mocks__/SettingsMockServer";
import { renderApp } from "../utils/CustomRenderUtils";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";
import { MemberType } from "../hooks/UseOktaMemberships";

import SenderModeBanner from "./SenderModeBanner";

describe("SenderModeBanner", () => {
    beforeAll(() => {
        settingsServer.listen();
    });
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());

    test("renders when sender is testing", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testSender",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
        });
        renderApp(<SenderModeBanner />);
        const text = await screen.findByText("Learn more about onboarding.");
        expect(text).toBeInTheDocument();
    });
});
