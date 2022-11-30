import { renderHook } from "@testing-library/react-hooks";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";
import { useSessionContext } from "../contexts/SessionContext";

import { MemberType } from "./UseOktaMemberships";
import {
    Organizations,
    useAdminSafeOrganizationName,
} from "./UseAdminSafeOrganizationName";

describe("useAdminSafeOrganizationName", () => {
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
        });
        const { activeMembership } = useSessionContext();
        const { result } = renderHook(() =>
            useAdminSafeOrganizationName(activeMembership?.parsedName)
        );
        expect(result.current).toEqual(undefined);
    });
    test("returns correct client organization", async () => {
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
        });
        const { activeMembership } = useSessionContext();
        const { result } = renderHook(() =>
            useAdminSafeOrganizationName(activeMembership?.parsedName)
        );
        expect(result.current).toEqual(activeMembership?.parsedName);
    });
    test("returns correct client organization for prime admins", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: Organizations.PRIMEADMINS,
                service: "testReceiver",
            },
            dispatch: () => {},
            initialized: true,
        });
        const { activeMembership } = useSessionContext();
        const { result } = renderHook(() =>
            useAdminSafeOrganizationName(activeMembership?.parsedName)
        );
        expect(result.current).toEqual(Organizations.IGNORE);
    });
});
