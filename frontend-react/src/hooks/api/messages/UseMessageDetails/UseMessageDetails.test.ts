import { waitFor } from "@testing-library/react";

import useMessageDetails from "./UseMessageDetails";
import { messageTrackerServer } from "../../../../__mockServers__/MessageTrackerMockServer";
import { renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

beforeAll(() => messageTrackerServer.listen());
afterEach(() => messageTrackerServer.resetHandlers());
afterAll(() => messageTrackerServer.close());

describe("useMessageDetails", () => {
    test("returns expected data values when fetching message details", async () => {
        mockSessionContentReturnValue({
            authState: {
                accessToken: { accessToken: "TOKEN" },
            } as any,
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
            },

            user: {
                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                isUserTransceiver: false,
            } as any,
        });

        const { result } = renderHook(() => useMessageDetails("11"));
        await waitFor(() => expect(result.current.messageDetails?.id).toEqual(11));
    });
});
