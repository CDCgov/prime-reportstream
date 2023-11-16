import { waitFor } from "@testing-library/react";

import { dummyReceivers, orgServer } from "../__mocks__/OrganizationMockServer";
import { MemberType } from "../utils/OrganizationUtils";
import { renderHook } from "../utils/Test/render";

import { useOrganizationReceivers } from "./UseOrganizationReceivers";
import { Organizations } from "./UseAdminSafeOrganizationName";

describe("useOrganizationReceivers", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no active membership parsed name", () => {
        const { result } = renderHook(() => useOrganizationReceivers(), {
            providers: {
                QueryClient: true,
                Session: {
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
                },
            },
        });
        expect(result.current.error).toBeNull();
        expect(result.current.data).toEqual(undefined);
    });
    test("returns correct organization receiver services", async () => {
        const { result } = renderHook(() => useOrganizationReceivers(), {
            providers: {
                QueryClient: true,
                Session: {
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
                },
            },
        });
        await waitFor(() =>
            expect(result.current.data).toEqual(dummyReceivers),
        );
        expect(result.current.isLoading).toEqual(false);
    });

    test("is disabled and returns undefined", () => {
        const { result } = renderHook(() => useOrganizationReceivers(), {
            providers: {
                QueryClient: true,
                Session: {
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
                },
            },
        });
        expect(result.current.error).toBeNull();
        expect(result.current.data).toEqual(undefined);
        expect(result.current.isLoading).toEqual(false);
        expect(result.current.isDisabled).toEqual(true);
    });
});
