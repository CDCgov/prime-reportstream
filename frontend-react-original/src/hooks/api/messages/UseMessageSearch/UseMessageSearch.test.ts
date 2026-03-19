import { act } from "@testing-library/react";

import useMessageSearch from "./UseMessageSearch";
import {
    messageTrackerServer,
    MOCK_MESSAGE_SENDER_DATA,
} from "../../../../__mockServers__/MessageTrackerMockServer";
import { renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

beforeAll(() => messageTrackerServer.listen());
afterEach(() => messageTrackerServer.resetHandlers());
afterAll(() => messageTrackerServer.close());

describe("useMessageSearch", () => {
    test("returns expected data values when fetching messages", async () => {
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

        const { result } = renderHook(() => useMessageSearch());
        let messages;
        await act(async () => {
            messages = await result.current.mutateAsync("alaska1");
            expect(messages.length).toEqual(3);
            expect(messages[0].reportId).toEqual(
                MOCK_MESSAGE_SENDER_DATA[0].reportId,
            );
            expect(messages[1].reportId).toEqual(
                MOCK_MESSAGE_SENDER_DATA[1].reportId,
            );
            expect(messages[2].reportId).toEqual(
                MOCK_MESSAGE_SENDER_DATA[2].reportId,
            );
        });
    });
});
