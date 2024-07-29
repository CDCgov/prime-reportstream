import { waitFor } from "@testing-library/react";
import useDeliveriesHistory from "./UseDeliveriesHistory";
import { deliveriesHistoryServer } from "../../../../__mockServers__/DeliveriesHistoryMockServer";
import { renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

describe("useDeliveriesHistory", () => {
    beforeAll(() => deliveriesHistoryServer.listen());
    afterEach(() => deliveriesHistoryServer.resetHandlers());
    afterAll(() => deliveriesHistoryServer.close());

    describe("with no Organization name", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: undefined,

                user: {
                    isUserAdmin: false,
                    isUserReceiver: false,
                    isUserSender: false,
                    isUserTransceiver: false,
                } as any,
            });
        });
        test("returns undefined", async () => {
            const { result } = renderHook(() => useDeliveriesHistory("testService"));
            await waitFor(() => expect(result.current).toBeNull());
        });
    });

    describe("with Organization and service name", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrg",
                    service: "testService",
                },

                user: {
                    isUserAdmin: false,
                    isUserReceiver: true,
                    isUserSender: false,
                    isUserTransceiver: false,
                } as any,
            });
        });

        test("returns receiver meta and deliveries", async () => {
            const { result } = renderHook(() => useDeliveriesHistory("testService"));
            await waitFor(() => expect(result.current.data!.data).toHaveLength(5));
            await waitFor(() => expect(Object.keys(result.current.data!.meta)).toHaveLength(5));
        });
    });
});
