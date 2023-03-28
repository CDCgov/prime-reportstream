import { renderHook, waitFor } from "@testing-library/react";
import { setupServer } from "msw/lib/node";

import { AppWrapper } from "../utils/CustomRenderUtils";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";
import { createRSMock } from "../__mocks__/DBMock";
import config from "../config";

import { MemberType } from "./UseOktaMemberships";
import { useOrganizationSettings } from "./UseOrganizationSettings";
import { Organizations } from "./UseAdminSafeOrganizationName";

const { db, reset } = createRSMock();

const orgServer = setupServer(
    ...db.organizationSettings.toEnhancedRestHandlers(config.API_ROOT, {
        getList: "/settings/organizations",
        get: "/settings/organizations/:name",
        post: "/settings/organizations/:name",
        put: "/settings/organizations/:name",
        delete: "/settings/organizations/:name",
    })
);

db.organizationSettings.createMany(10, [{ name: "testOrg" }]);

describe("useOrganizationSettings", () => {
    beforeAll(() => {
        orgServer.listen();
    });
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => {
        orgServer.close();
        reset();
    });
    describe("with no Organization name", () => {
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
            });
        });

        test("returns undefined", () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                wrapper: AppWrapper(),
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
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: true,
            });
        });

        test("returns correct organization settings", async () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                wrapper: AppWrapper(),
            });
            await waitFor(() =>
                expect(result.current.data?.name).toEqual("testOrg")
            );
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
                isUserAdmin: true,
                isUserReceiver: false,
                isUserSender: false,
            });
        });

        test("is disabled", async () => {
            const { result } = renderHook(() => useOrganizationSettings(), {
                wrapper: AppWrapper(),
            });
            expect(result.current.fetchStatus).toEqual("idle");
            expect(result.current.status).toEqual("loading");
            expect(result.current.isInitialLoading).toEqual(false);
        });
    });
});
