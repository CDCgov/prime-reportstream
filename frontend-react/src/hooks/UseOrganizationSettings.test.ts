import { renderHook } from "@testing-library/react-hooks";

import { QueryWrapper } from "../utils/CustomRenderUtils";
import { fakeOrg, orgServer } from "../__mocks__/OrganizationMockServer";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";

import { MemberType } from "./UseOktaMemberships";
import { useOrganizationSettings } from "./UseOrganizationSettings";
import { Organizations } from "./UseAdminSafeOrganizationName";

describe("useOrganizationSettings", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    describe("with no Organization name", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                oktaToken: {
                    accessToken: "TOKEN",
                },
                activeMembership: undefined,
                dispatch: () => {},
                initialized: true,
            });
        });

        test("returns undefined", () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                wrapper: QueryWrapper(),
            });
            expect(result.current.data).toEqual(undefined);
            expect(result.current.isLoading).toEqual(true);
        });
    });

    describe("with a non-admin Organization name", () => {
        beforeEach(() => {
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
            });
        });

        test("returns correct organization settings", async () => {
            const { result, waitForNextUpdate } = renderHook(
                () => useOrganizationSettings(),
                { wrapper: QueryWrapper() }
            );
            await waitForNextUpdate();
            expect(result.current.data).toEqual(fakeOrg);
            expect(result.current.isLoading).toEqual(false);
        });
    });

    describe("with an admin Organization name", () => {
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
            });
        });

        test("is disabled", async () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                wrapper: QueryWrapper(),
            });
            expect(result.current.fetchStatus).toEqual("idle");
            expect(result.current.status).toEqual("loading");
            expect(result.current.isInitialLoading).toEqual(false);
        });
    });
});
