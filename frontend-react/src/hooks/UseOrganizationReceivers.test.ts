import { waitFor } from "@testing-library/react";

import { renderHook } from "../utils/CustomRenderUtils";
import { dummyReceivers, orgServer } from "../__mocks__/OrganizationMockServer";
import { mockSessionContentReturnValue } from "../contexts/__mocks__/SessionContext";
import { MemberType } from "../utils/OrganizationUtils";

import { useOrganizationReceivers } from "./UseOrganizationReceivers";
import { Organizations } from "./UseAdminSafeOrganizationName";

describe("useOrganizationReceivers", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns null if no active membership parsed name", async () => {
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
        const { result } = renderHook(() => useOrganizationReceivers());
        await waitFor(() => expect(result.current.data).toBeNull());
    });
    test("returns correct organization receiver services", async () => {
        mockSessionContentReturnValue({
            authState: {
                accessToken: { accessToken: "TOKEN" },
            } as any,
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
                service: "testReceiver",
            },

            user: {
                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                isUserTransceiver: false,
            } as any,
        });
        const { result } = renderHook(() => useOrganizationReceivers());
        await waitFor(() =>
            expect(result.current.data).toEqual(dummyReceivers),
        );
        expect(result.current.isLoading).toEqual(false);
    });

    test("is disabled and returns undefined", async () => {
        mockSessionContentReturnValue({
            authState: {
                accessToken: { accessToken: "TOKEN" },
            } as any,
            activeMembership: {
                memberType: MemberType.PRIME_ADMIN,
                parsedName: Organizations.PRIMEADMINS,
            },
            user: {
                isUserAdmin: true,
                isUserReceiver: false,
                isUserSender: false,
            },
        } as any);
        const { result } = renderHook(() => useOrganizationReceivers());
        await waitFor(() => expect(result.current.data).toBeNull());
        expect(result.current.isLoading).toEqual(false);
        expect(result.current.isDisabled).toEqual(true);
    });
});
