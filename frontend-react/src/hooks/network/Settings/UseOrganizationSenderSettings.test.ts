import { renderHook, waitFor } from "@testing-library/react";

import { AppWrapper } from "../../../utils/CustomRenderUtils";
import {
    dummySender,
    orgServer,
} from "../../../__mocks__/OrganizationMockServer";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MembershipSettings, MemberType } from "../../UseOktaMemberships";

import { useOrganizationSenderSettings } from "./UseOrganizationSenderSettings";

describe("useOrganizationSenderSettings", () => {
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
        });
        const { result } = renderHook(() => useOrganizationSenderSettings(), {
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
        });
        const { result } = renderHook(() => useOrganizationSenderSettings(), {
            wrapper: AppWrapper(),
        });
        await waitFor(() =>
            expect(result.current.senderDetail).toEqual(dummySender)
        );
        expect(result.current.senderIsLoading).toEqual(false);
    });
});
