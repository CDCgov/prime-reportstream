import { waitFor } from "@testing-library/react";

import useOrganizationPublicKeys from "./UseOrganizationPublicKeys";
import { dummyPublicKey, orgServer } from "../../../../__mockServers__/OrganizationMockServer";
import { renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

describe("useOrganizationPublicKeys", () => {
    beforeAll(() => orgServer.listen());
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

        test("returns undefined", async () => {
            const { result } = renderHook(() => useOrganizationPublicKeys());
            await waitFor(() => expect(result.current.data).toBeNull());
        });
    });

    describe("with Organization name", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.SENDER,
                    parsedName: "testOrg",
                    service: "testOrgPublicKey",
                },

                user: {
                    isUserAdmin: false,
                    isUserReceiver: false,
                    isUserSender: true,
                    isUserTransceiver: false,
                } as any,
            });
        });

        test("returns organization public keys", async () => {
            const { result } = renderHook(() => useOrganizationPublicKeys());
            await waitFor(() => expect(result.current.data).toEqual(dummyPublicKey));
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
