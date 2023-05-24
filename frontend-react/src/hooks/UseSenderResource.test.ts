import { renderHook, waitFor } from "@testing-library/react";

import { AppWrapper } from "../utils/CustomRenderUtils";
import { dummySender, orgServer } from "../__mocks__/OrganizationMockServer";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";

import { useSenderResource } from "./UseSenderResource";
import { MembershipSettings, MemberType } from "./UseOktaMemberships";

describe("useSenderResource", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no sender available on membership", () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.NON_STAND,
                service: undefined,
            } as MembershipSettings,
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
            environment: "test",
        });
        const { result } = renderHook(() => useSenderResource(), {
            wrapper: AppWrapper(),
        });
        expect(result.current.senderDetail).toEqual(undefined);
        expect(result.current.senderIsLoading).toEqual(true);
    });
    test("returns correct sender match", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testSender",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
            environment: "test",
        });
        const { result } = renderHook(() => useSenderResource(), {
            wrapper: AppWrapper(),
        });
        await waitFor(() =>
            expect(result.current.senderDetail).toEqual(dummySender)
        );
        expect(result.current.senderIsLoading).toEqual(false);
    });
});
