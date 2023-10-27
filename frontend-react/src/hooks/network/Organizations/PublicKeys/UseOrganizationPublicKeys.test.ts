import { waitFor } from "@testing-library/react";

import { renderHook } from "../../../../utils/CustomRenderUtils";
import { mockSessionContentReturnValue } from "../../../../contexts/__mocks__/SessionContext";
import {
    dummyPublicKey,
    orgServer,
} from "../../../../__mocks__/OrganizationMockServer";
import { MemberType } from "../../../../utils/OrganizationUtils";

import useOrganizationPublicKeys from "./UseOrganizationPublicKeys";

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
                } as any,
            });
        });

        test("returns undefined", () => {
            const { result } = renderHook(() => useOrganizationPublicKeys());
            expect(result.current.data).toEqual(undefined);
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
                } as any,
            });
        });

        test("returns organization public keys", async () => {
            const { result } = renderHook(() => useOrganizationPublicKeys());
            await waitFor(() =>
                expect(result.current.data).toEqual(dummyPublicKey),
            );
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
