import { waitFor } from "@testing-library/react";

import {
    dummyPublicKey,
    orgServer,
} from "../../../../__mocks__/OrganizationMockServer";
import { MemberType } from "../../../../utils/OrganizationUtils";
import { renderHook } from "../../../../utils/Test/render";

import useOrganizationPublicKeys from "./UseOrganizationPublicKeys";

describe("useOrganizationPublicKeys", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    describe("with no Organization name", () => {
        test("returns undefined", () => {
            const { result } = renderHook(() => useOrganizationPublicKeys());
            expect(result.current.data).toEqual(undefined);
        });
    });

    describe("with Organization name", () => {
        test("returns organization public keys", async () => {
            const { result } = renderHook(() => useOrganizationPublicKeys(), {
                providers: {
                    Session: {
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
                        },
                    },
                },
            });
            await waitFor(() =>
                expect(result.current.data).toEqual(dummyPublicKey),
            );
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
