import { mockSessionContentReturnValue } from "../../../contexts/__mocks__/SessionContext";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { renderHook } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";

import { useOrgDeliveries } from "./DeliveryHooks";

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
});
