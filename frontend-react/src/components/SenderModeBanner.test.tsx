import { screen } from "@testing-library/react";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import {
    setStoredSenderName,
    setStoredOrg,
} from "../contexts/SessionStorageTools";
import { renderWithSession } from "../utils/CustomRenderUtils";
import * as SessionContext from "../contexts/SessionContext";
import { MembershipController, MemberType } from "../hooks/UseGroups";
import { parseOrgName } from "../utils/OrganizationUtils";
import { StoreController } from "../hooks/UseSessionStorage";

import SenderModeBanner from "./SenderModeBanner";

const mockContext = jest.spyOn(SessionContext, "useSessionContext");

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
        mockContext.mockReturnValue({
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.SENDER,
                        parsedName: parseOrgName("DHSender_all-in-one-health"),
                    },
                },
            } as MembershipController,
            store: {} as StoreController,
        });
        renderWithSession(<SenderModeBanner />);
        const text = await screen.findByText("Learn more about onboarding.");
        expect(text).toBeInTheDocument();
    });
});
