import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { Organizations } from "../../UseAdminSafeOrganizationName";
import { renderHook } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";
import { mockUseSessionContext } from "../contexts/Session/__mocks__";

import { useOrgDeliveries } from "./DeliveryHooks";

describe("useReportsList", () => {
    describe("when requesting as a receiver", () => {
        beforeAll(() => {
            deliveryServer.listen();
            mockUseSessionContext.mockReturnValue({
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
        afterEach(() => deliveryServer.resetHandlers());
        afterAll(() => deliveryServer.close());

        test("fetchResults returns an array of deliveries", async () => {
            const { result } = renderHook(() =>
                useOrgDeliveries("testService"),
            );
            const results = await result.current.fetchResults(" ", 10);

            expect(results).toHaveLength(3);
        });
    });

    describe("when requesting as an admin", () => {
        beforeAll(() => {
            deliveryServer.listen();
            mockUseSessionContext.mockReturnValue({
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
        afterEach(() => deliveryServer.resetHandlers());
        afterAll(() => deliveryServer.close());

        test("fetchResults returns an empty array", async () => {
            const { result } = renderHook(() =>
                useOrgDeliveries("testService"),
            );
            const results = await result.current.fetchResults(" ", 10);

            expect(results).toHaveLength(0);
        });
    });
});
