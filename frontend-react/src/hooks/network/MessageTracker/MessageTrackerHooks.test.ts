import { act, renderHook } from "@testing-library/react-hooks";

import {
    messageTrackerServer,
    MOCK_MESSAGE_SENDER_DATA,
} from "../../../__mocks__/MessageTrackerMockServer";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { QueryWrapper } from "../../../utils/CustomRenderUtils";

import { useMessageSearch, useMessageDetails } from "./MessageTrackerHooks";

beforeAll(() => messageTrackerServer.listen());
afterEach(() => messageTrackerServer.resetHandlers());
afterAll(() => messageTrackerServer.close());

describe("useMessageSearch", () => {
    test("returns expected data values when fetching messages", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
            },
            dispatch: () => {},
            initialized: true,
        });

        const { result } = renderHook(() => useMessageSearch(), {
            wrapper: QueryWrapper(),
        });
        let messages;
        await act(async () => {
            messages = await result.current.search("alaska1");
            expect(messages.length).toEqual(3);
            expect(messages[0].reportId).toEqual(
                MOCK_MESSAGE_SENDER_DATA[0].reportId
            );
            expect(messages[1].reportId).toEqual(
                MOCK_MESSAGE_SENDER_DATA[1].reportId
            );
            expect(messages[2].reportId).toEqual(
                MOCK_MESSAGE_SENDER_DATA[2].reportId
            );
        });
    });
});

describe("useMessageDetails", () => {
    test("returns expected data values when fetching message details", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
            },
            dispatch: () => {},
            initialized: true,
        });

        const { result, waitForNextUpdate } = renderHook(
            () => useMessageDetails("11"),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.messageDetails?.id).toEqual(11);
    });
});
