import useOrgDeliveries from "./UseOrgDeliveries";
import { deliveryServer } from "../../../../__mockServers__/DeliveriesMockServer";
import { renderHook } from "../../../../utils/CustomRenderUtils";
import { MemberType } from "../../../../utils/OrganizationUtils";
import { Organizations } from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../../contexts/Session/__mocks__/useSessionContext")
>("../../../../contexts/Session/useSessionContext");

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("returns expected data", async () => {
        mockSessionContentReturnValue({
            authState: {
                accessToken: { accessToken: "TOKEN" },
            } as any,
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
            },

            user: {
                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                isUserTransceiver: false,
            } as any,
        });

        const { result } = renderHook(() => useOrgDeliveries("testService"));
        const fetchResults = await result.current.fetchResults(" ", 10);
        expect(fetchResults).toHaveLength(3);
        expect(result.current.filterManager.pageSettings.currentPage).toEqual(
            1,
        );
        expect(result.current.filterManager.pageSettings.size).toEqual(10);
        expect(result.current.filterManager.rangeSettings.from).toEqual(
            "2000-01-01T00:00:00.000Z",
        );
        expect(result.current.filterManager.rangeSettings.to).toEqual(
            "3000-01-01T00:00:00.000Z",
        );
        expect(result.current.filterManager.sortSettings.order).toEqual("DESC");
    });
    describe("when requesting as a receiver", () => {
        beforeAll(() => deliveryServer.listen());
        afterEach(() => deliveryServer.resetHandlers());
        afterAll(() => deliveryServer.close());
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.RECEIVER,
                    parsedName: "testOrg",
                },

                user: {
                    isUserAdmin: false,
                    isUserReceiver: true,
                    isUserSender: false,
                    isUserTransceiver: false,
                } as any,
            });
        });

        test("fetchResults returns an array of deliveries", async () => {
            const { result } = renderHook(() =>
                useOrgDeliveries("testService"),
            );
            const results = await result.current.fetchResults(" ", 10);

            expect(results).toHaveLength(3);
        });
    });

    describe("when requesting as an admin", () => {
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.PRIME_ADMIN,
                    parsedName: Organizations.PRIMEADMINS,
                },

                user: {
                    isUserAdmin: true,
                    isUserReceiver: false,
                    isUserSender: false,
                    isUserTransceiver: false,
                } as any,
            });
        });

        test("fetchResults returns an empty array", async () => {
            const { result } = renderHook(() =>
                useOrgDeliveries("testService"),
            );
            const results = await result.current.fetchResults(" ", 10);

            expect(results).toHaveLength(0);
        });
    });
});
