import { waitFor } from "@testing-library/react";

import { renderHook } from "../../../../utils/CustomRenderUtils";
import {
    dummyPublicKey,
    orgServer,
} from "../../../../__mocks__/OrganizationMockServer";
import { MemberType } from "../../../../utils/OrganizationUtils";
import { useSessionContext } from "../../../../contexts/SessionContext";
import { defaultCtx } from "../../../../contexts/__mocks__/SessionContext";

import useOrganizationPublicKeys from "./UseOrganizationPublicKeys";

vi.mock("../../../../contexts/SessionContext");

const mockUseSessionContext = vi.mocked(useSessionContext);

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
        beforeEach(() => {
            mockUseSessionContext.mockResolvedValue({
                ...defaultCtx,
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
            await waitFor(() =>
                expect(result.current.data).toEqual(dummyPublicKey),
            );
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
