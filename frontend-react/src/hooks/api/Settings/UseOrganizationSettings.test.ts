import { renderHook } from "@testing-library/react-hooks";

import { QueryWrapper } from "../../../utils/CustomRenderUtils";
import {
    fakeOrg,
    orgServer,
} from "../../../config/api/__mocks__/OrganizationMockServer";
import { MemberType } from "../../UseOktaMemberships";
import { mockAuthReturnValue } from "../__mocks__/OktaAuth";

import { useOrganizationSettings } from "./UseOrganizationSettings";

describe("useOrganizationSettings", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no active membership parsed name", () => {
        mockAuthReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: undefined,
            dispatch: () => {},
            initialized: true,
        });
        const { result } = renderHook(() => useOrganizationSettings(), {
            wrapper: QueryWrapper(),
        });
        expect(result.current.data).toEqual(undefined);
        expect(result.current.isLoading).toEqual(true);
    });
    test("returns correct organization settings", async () => {
        mockAuthReturnValue({
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
        });
        const { result, waitForNextUpdate } = renderHook(
            () => useOrganizationSettings(),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual(fakeOrg);
        expect(result.current.isLoading).toEqual(false);
    });
});
