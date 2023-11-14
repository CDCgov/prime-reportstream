import { waitFor } from "@testing-library/react";

import { renderHook } from "../utils/CustomRenderUtils";
import { fakeOrg, orgServer } from "../__mocks__/OrganizationMockServer";
import { MemberType } from "../utils/OrganizationUtils";

import { useOrganizationSettings } from "./UseOrganizationSettings";
import { Organizations } from "./UseAdminSafeOrganizationName";

describe("useOrganizationSettings", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    describe("with no Organization name", () => {
        test("returns undefined", async () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                providers: {
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
            await waitFor(() => expect(result.current.isLoading).toBeFalsy());
            expect(result.current.data).toEqual(undefined);
        });
    });

    describe("with a non-admin Organization name", () => {
        test("returns correct organization settings", async () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                providers: {
                    Session: {
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
                    },
                },
            });
            await waitFor(() => expect(result.current.isLoading).toBeFalsy());
            await waitFor(() => expect(result.current.data).toEqual(fakeOrg));
            expect(result.current.isLoading).toEqual(false);
        });
    });

    describe("with an admin Organization name", () => {
        test("is disabled", async () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                providers: {
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
                            isUserTransceiver: false,
                        } as any,
                    },
                },
            });
            expect(result.current.fetchStatus).toEqual("idle");
            expect(result.current.status).toEqual("pending");
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
