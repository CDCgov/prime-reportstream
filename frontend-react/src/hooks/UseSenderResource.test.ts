import { waitFor } from "@testing-library/react";

import { AppWrapper, renderHook } from "../utils/CustomRenderUtils";
import { dummySender, orgServer } from "../__mocks__/OrganizationMockServer";
import { mockSessionContentReturnValue } from "../contexts/__mocks__/SessionContext";
import { MemberType, MembershipSettings } from "../utils/OrganizationUtils";

import useSenderResource from "./UseSenderResource";

describe("useSenderResource", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no sender available on membership", () => {
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
        const { result } = renderHook(() => useSenderResource());
        expect(result.current.data).toEqual(undefined);
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
        const { result } = renderHook(() => useSenderResource(), {
            wrapper: AppWrapper(),
        });
        await waitFor(() => expect(result.current.data).toEqual(dummySender));
        expect(result.current.isLoading).toEqual(false);
    });
});
