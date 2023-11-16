import { waitFor } from "@testing-library/react";

import { dummySenders, orgServer } from "../__mocks__/OrganizationMockServer";
import { MemberType } from "../utils/OrganizationUtils";
import { renderHook } from "../utils/Test/render";

import useOrganizationSenders from "./UseOrganizationSenders";

describe("useOrganizationSenders", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    describe("with no Organization name", () => {
        test("returns undefined", () => {
            const { result } = renderHook(() => useOrganizationSenders(), {
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
            expect(result.current.data).toEqual(undefined);
        });
    });

    describe("returns organization senders", () => {
        test("returns correct organization settings", async () => {
            const { result } = renderHook(() => useOrganizationSenders(), {
                providers: {
                    QueryClient: true,
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
            await waitFor(() =>
                expect(result.current.data).toEqual(dummySenders),
            );
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
