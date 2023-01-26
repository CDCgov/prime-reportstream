import { renderHook } from "@testing-library/react-hooks";

import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../config/api/__mocks__/DeliveriesMockServer";
import { mockAuthReturnValue } from "../__mocks__/OktaAuth";

import { useOrgDeliveries } from "./UseOrganizationDeliveries";

describe(useOrgDeliveries.name, () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportsList", async () => {
        mockAuthReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
            },
            dispatch: () => {},
            initialized: true,
        });

        const { result } = renderHook(() => useOrgDeliveries("testService"));
        const fetchResults = await result.current.fetchResults(" ", 10);
        expect(fetchResults).toHaveLength(3);
        expect(result.current.filterManager.pageSettings.currentPage).toEqual(
            1
        );
        expect(result.current.filterManager.pageSettings.size).toEqual(10);
        expect(result.current.filterManager.rangeSettings.from).toEqual(
            "2000-01-01T00:00:00.000Z"
        );
        expect(result.current.filterManager.rangeSettings.to).toEqual(
            "3000-01-01T00:00:00.000Z"
        );
        expect(result.current.filterManager.sortSettings.order).toEqual("DESC");
    });
});
