import { act, waitFor } from "@testing-library/react";

import {
    messageTrackerServer,
    MOCK_MESSAGE_SENDER_DATA,
} from "../../../__mocks__/MessageTrackerMockServer";
import { MemberType } from "../../../utils/OrganizationUtils";
import { renderHook } from "../../../utils/Test/render";

import { useMessageSearch, useMessageDetails } from "./MessageTrackerHooks";

beforeAll(() => messageTrackerServer.listen());
afterEach(() => messageTrackerServer.resetHandlers());
afterAll(() => messageTrackerServer.close());

describe("useMessageSearch", () => {
    test("returns expected data values when fetching messages", async () => {
        const { result } = renderHook(() => useMessageSearch(), {
            providers: {
                Session: {
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
                },
            },
        });
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

describe("useMessageDetails", () => {
    test("returns expected data values when fetching message details", async () => {
        const { result } = renderHook(() => useMessageDetails("11"), {
            providers: {
                Session: {
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
                },
            },
        });
        await waitFor(() =>
            expect(result.current.messageDetails?.id).toEqual(11),
        );
    });
});
