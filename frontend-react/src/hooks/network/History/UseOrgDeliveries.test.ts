import { mockSessionContentReturnValue } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { Organizations } from "../../UseAdminSafeOrganizationName";
import { renderHook } from "../../../utils/CustomRenderUtils";

import { useOrgDeliveries } from "./DeliveryHooks";

describe("useReportsList", () => {
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

                isUserAdmin: false,
                isUserReceiver: true,
                isUserSender: false,
                environment: "test",
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
        beforeAll(() => deliveryServer.listen());
        afterEach(() => deliveryServer.resetHandlers());
        afterAll(() => deliveryServer.close());
        beforeEach(() => {
            mockSessionContentReturnValue({
                authState: {
                    accessToken: { accessToken: "TOKEN" },
                } as any,
                activeMembership: {
                    memberType: MemberType.PRIME_ADMIN,
                    parsedName: Organizations.PRIMEADMINS,
                },

                isUserAdmin: true,
                isUserReceiver: false,
                isUserSender: false,
                environment: "test",
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
