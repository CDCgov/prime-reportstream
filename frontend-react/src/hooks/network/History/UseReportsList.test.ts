import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { MemberType } from "../../../utils/OrganizationUtils";
import { renderHook } from "../../../utils/Test/render";

import { useOrgDeliveries } from "./DeliveryHooks";

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("returns expected data", async () => {
        const { result } = renderHook(() => useOrgDeliveries("testService"), {
            providers: {
                QueryClient: true,
                Session: {
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
                },
            },
        });
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
