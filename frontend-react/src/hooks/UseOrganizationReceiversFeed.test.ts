import { act, renderHook, waitFor } from "@testing-library/react";

import { AppWrapper } from "../utils/CustomRenderUtils";
import {
    dummyActiveReceiver,
    dummyReceivers,
    orgServer,
} from "../__mocks__/OrganizationMockServer";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";

import { MemberType } from "./UseOktaMemberships";
import { useOrganizationReceiversFeed } from "./UseOrganizationReceiversFeed";
import { Organizations } from "./UseAdminSafeOrganizationName";

describe("useOrganizationReceiversFeed", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    describe("with no active membership parsed name", () => {
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

        test("is disabled and returns an empty array", () => {
            const { result } = renderHook(
                () => useOrganizationReceiversFeed(),
                {
                    wrapper: AppWrapper(),
                },
            );
            expect(result.current.services).toEqual([]);
            expect(result.current.setActiveService).toBeDefined();
            expect(result.current.activeService).toEqual(undefined);
            expect(result.current.loadingServices).toEqual(false);
            expect(result.current.isDisabled).toEqual(true);
        });
    });

    describe("with an admin parsed name", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: {
                    memberType: MemberType.PRIME_ADMIN,
                    parsedName: Organizations.PRIMEADMINS,
                },
                dispatch: () => {},
                initialized: true,
                isUserAdmin: true,
                isUserReceiver: false,
                isUserSender: false,
                environment: "test",
            });
        });

        test("is disabled and returns an empty array", () => {
            const { result } = renderHook(
                () => useOrganizationReceiversFeed(),
                {
                    wrapper: AppWrapper(),
                },
            );
            expect(result.current.services).toEqual([]);
            expect(result.current.setActiveService).toBeDefined();
            expect(result.current.activeService).toEqual(undefined);
            expect(result.current.loadingServices).toEqual(false);
            expect(result.current.isDisabled).toEqual(true);
        });
    });

    describe("with a non-admin parsed name", () => {
        beforeEach(() => {
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
        });

        test("returns correct organization receiver services feed", async () => {
            const { result } = renderHook(
                () => useOrganizationReceiversFeed(),
                { wrapper: AppWrapper() },
            );
            await waitFor(() =>
                expect(result.current.services).toEqual(dummyReceivers),
            );
            expect(result.current.setActiveService).toBeDefined();
            await waitFor(() =>
                expect(result.current.activeService).toEqual(
                    dummyActiveReceiver,
                ),
            );
            expect(result.current.loadingServices).toEqual(false);
        });
    });

    test("setActiveService sets an active receiver", async () => {
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
        const { result } = renderHook(() => useOrganizationReceiversFeed(), {
            wrapper: AppWrapper(),
        });
        await waitFor(() => expect(result.current.activeService).toBeDefined());
        expect(result.current.activeService).toEqual({
            name: "elr-0",
            organizationName: "testOrg",
        });
        act(() => result.current.setActiveService(result.current.services[1]));
        expect(result.current.activeService).toEqual({
            name: "elr-1",
            organizationName: "testOrg",
        });
    });
});
