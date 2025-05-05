import { waitFor } from "@testing-library/react";

import useOrganizationSender from "./UseOrganizationSender";
import { dummySender, orgServer } from "../../../../__mockServers__/OrganizationMockServer";
import { AppWrapper, renderHook } from "../../../../utils/CustomRenderUtils";
import { MembershipSettings, MemberType } from "../../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

describe("useSenderResource", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns null if no sender available on membership", async () => {
        mockSessionContentReturnValue({
            authState: {
                accessToken: { accessToken: "TOKEN" },
            } as any,
            activeMembership: {
                memberType: MemberType.NON_STAND,
                service: undefined,
            } as MembershipSettings,

            user: {
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: true,
                isUserTransceiver: false,
            } as any,
        });
        const { result } = renderHook(() => useOrganizationSender());
        await waitFor(() => expect(result.current.data).toBeNull());
    });
    test("returns correct sender match", async () => {
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
        const { result } = renderHook(() => useOrganizationSender(), {
            wrapper: AppWrapper(),
        });
        await waitFor(() => expect(result.current.data).toEqual(dummySender));
        expect(result.current.isLoading).toEqual(false);
    });
});
