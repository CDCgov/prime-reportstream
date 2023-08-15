import { renderHook } from "@testing-library/react";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";

import { useOrgDeliveries } from "./DeliveryHooks";

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("returns expected data", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: true,
            isUserSender: false,
            environment: "test",
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
