import { waitFor } from "@testing-library/react";

import { AppWrapper, renderHook } from "../utils/CustomRenderUtils";
import { dummySenders, orgServer } from "../__mocks__/OrganizationMockServer";
import { mockSessionContentReturnValue } from "../contexts/__mocks__/SessionContext";
import { MemberType } from "../utils/OrganizationUtils";

import useOrganizationSenders from "./UseOrganizationSenders";

describe("useOrganizationSenders", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    describe("with no Organization name", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: undefined,

                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: false,
                environment: "test",
            });
        });

        test("returns undefined", () => {
            const { result } = renderHook(() => useOrganizationSenders(), {
                wrapper: AppWrapper(),
            });
            expect(result.current.data).toEqual(undefined);
            expect(result.current.isLoading).toEqual(true);
        });
    });

    describe("returns organization senders", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.SENDER,
                    parsedName: "testOrg",
                    service: "testSender",
                },

                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: true,
                environment: "test",
            });
        });

        test("returns correct organization settings", async () => {
            const { result } = renderHook(() => useOrganizationSenders(), {
                wrapper: AppWrapper(),
            });
            await waitFor(() =>
                expect(result.current.data).toEqual(dummySenders),
            );
            expect(result.current.isLoading).toEqual(false);
        });
    });
});
