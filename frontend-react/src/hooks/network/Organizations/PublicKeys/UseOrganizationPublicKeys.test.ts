import { renderHook, waitFor } from "@testing-library/react";

import { AppWrapper } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../UseOktaMemberships";
import { mockSessionContext } from "../../../../contexts/__mocks__/SessionContext";
import {
    dummyPublicKey,
    orgServer,
} from "../../../../__mocks__/OrganizationMockServer";

import useOrganizationPublicKeys from "./UseOrganizationPublicKeys";

describe("useOrganizationPublicKeys", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    describe("with no Organization name", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: undefined,
                dispatch: () => {},
                initialized: true,
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: false,
                environment: "test",
            });
        });

        test("returns undefined", () => {
            const { result } = renderHook(() => useOrganizationPublicKeys(), {
                wrapper: AppWrapper(),
            });
            expect(result.current.data).toEqual(undefined);
            expect(result.current.isLoading).toEqual(true);
        });
    });

    describe("with Organization name", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: {
                    memberType: MemberType.SENDER,
                    parsedName: "testOrg",
                    service: "testOrgPublicKey",
                },
                dispatch: () => {},
                initialized: true,
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: true,
                environment: "test",
            });
        });

        test("returns organization public keys", async () => {
            const { result } = renderHook(() => useOrganizationPublicKeys(), {
                wrapper: AppWrapper(),
            });
            await waitFor(() =>
                expect(result.current.data).toEqual(dummyPublicKey),
            );
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
