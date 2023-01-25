import { renderHook } from "@testing-library/react-hooks";

import { QueryWrapper } from "../../../utils/CustomRenderUtils";
import {
    dummySender,
    orgServer,
} from "../../../config/api/__mocks__/OrganizationMockServer";
import { MembershipSettings, MemberType } from "../../UseOktaMemberships";
import { mockAuthReturnValue } from "../__mocks__/OktaAuth";

import { useOrganizationSenderSettings } from "./UseOrganizationSenderSettings";

describe("useOrganizationSenders", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("returns undefined if no sender available on membership", () => {
        mockAuthReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.NON_STAND,
                service: undefined,
            } as MembershipSettings,
            dispatch: () => {},
            initialized: true,
        });
        const { result } = renderHook(() => useOrganizationSenderSettings(), {
            wrapper: QueryWrapper(),
        });
        expect(result.current.data).toEqual(undefined);
        expect(result.current.isLoading).toEqual(true);
    });
    test("returns correct sender match", async () => {
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
            () => useOrganizationSenderSettings(),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual(dummySender);
        expect(result.current.isLoading).toEqual(false);
    });
});
