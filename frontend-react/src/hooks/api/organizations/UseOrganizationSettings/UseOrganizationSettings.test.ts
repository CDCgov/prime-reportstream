import { waitFor } from "@testing-library/react";

import useOrganizationSettings from "./UseOrganizationSettings";
import {
    fakeOrg,
    orgServer,
} from "../../../../__mockServers__/OrganizationMockServer";
import { renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";
import { Organizations } from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

describe("useOrganizationSettings", () => {
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
            const { result } = renderHook(() => useOrganizationSettings());
            await waitFor(() => expect(result.current.data).toBeNull());
        });
    });

    describe("with a non-admin Organization name", () => {
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
            const { result } = renderHook(() => useOrganizationSettings());
            await waitFor(() => expect(result.current.isLoading).toBeFalsy());
            await waitFor(() => expect(result.current.data).toEqual(fakeOrg));
            expect(result.current.isLoading).toEqual(false);
        });
    });

    describe("with an admin Organization name", () => {
        beforeEach(() => {
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
                    isUserTransceiver: false,
                } as any,
            });
        });

        test("is disabled", async () => {
            const { result } = renderHook(() => useOrganizationSettings());
            await waitFor(() => expect(result.current.data).toBeNull());
            expect(result.current.fetchStatus).toEqual("idle");
            expect(result.current.status).toEqual("success");
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
