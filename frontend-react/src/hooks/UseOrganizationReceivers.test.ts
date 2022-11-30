import { renderHook } from "@testing-library/react-hooks";

import { QueryWrapper } from "../utils/CustomRenderUtils";
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
        });
        const { result } = renderHook(() => useOrganizationReceivers(), {
            wrapper: QueryWrapper(),
        });
        expect(result.current.data).toEqual(undefined);
        expect(result.current.isLoading).toEqual(true);
    });
    test("returns correct organization receivers", async () => {
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
        const { result, waitForNextUpdate } = renderHook(
            () => useOrganizationReceivers(),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual(dummyReceivers);
        expect(result.current.isLoading).toEqual(false);
    });
});
