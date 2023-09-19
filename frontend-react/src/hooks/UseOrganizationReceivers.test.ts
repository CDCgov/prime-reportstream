import { renderHook, waitFor } from "@testing-library/react";

import { AppWrapper } from "../utils/CustomRenderUtils";
import { dummyReceivers, orgServer } from "../__mocks__/OrganizationMockServer";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";

import { MemberType } from "./UseOktaMemberships";
import { useOrganizationReceivers } from "./UseOrganizationReceivers";

describe("useOrganizationReceivers", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no active membership parsed name", () => {
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
        const { result } = renderHook(() => useOrganizationReceivers(), {
            wrapper: AppWrapper(),
        });
        expect(result.current.data).toEqual(undefined);
        expect(result.current.isLoading).toEqual(true);
    });
    test("returns correct organization receiver services", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
                service: "testReceiver",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: true,
            isUserSender: false,
            environment: "test",
        });
        const { result } = renderHook(() => useOrganizationReceivers(), {
            wrapper: AppWrapper(),
        });
        await waitFor(() =>
            expect(result.current.data).toEqual(dummyReceivers),
        );
        expect(result.current.isLoading).toEqual(false);
    });
});
