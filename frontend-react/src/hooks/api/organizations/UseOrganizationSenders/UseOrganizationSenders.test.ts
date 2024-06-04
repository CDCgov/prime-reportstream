import { waitFor } from "@testing-library/react";

import useOrganizationSenders from "./UseOrganizationSenders";
import {
    dummySenders,
    orgServer,
} from "../../../../__mockServers__/OrganizationMockServer";
import { AppWrapper, renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

describe("useOrganizationSenders", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
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

        test("returns null", async () => {
            const { result } = renderHook(() => useOrganizationSenders());
            await waitFor(() => expect(result.current.data).toBeNull());
        });
    });

    describe("returns organization senders", () => {
        beforeEach(() => {
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
        });

        test("returns correct organization settings", async () => {
            const { result } = renderHook(() => useOrganizationSenders(), {
                wrapper: AppWrapper(),
            });
            await waitFor(() =>
                expect(result.current.data).toEqual(dummySenders),
            );
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
