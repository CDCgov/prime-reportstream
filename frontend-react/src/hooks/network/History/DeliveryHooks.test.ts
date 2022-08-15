import { act, renderHook } from "@testing-library/react-hooks";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MembershipController, MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";

import { useReportsDetail, useReportsList } from "./DeliveryHooks";

describe("ReportsHooks", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportsList", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrg",
                        senderName: undefined,
                    },
                },
            } as MembershipController,
        });
        const { result, waitForNextUpdate } = renderHook(() =>
            useReportsList("testOrg", "testService")
        );
        expect(result.current.loading).toBeFalsy();
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.loading).toBeFalsy();
        expect(result.current.data).toHaveLength(3);
    });
    test("useReportDetail", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrg",
                        senderName: undefined,
                    },
                },
            } as MembershipController,
        });
        const { result, waitForNextUpdate } = renderHook(() =>
            useReportsDetail("123")
        );
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.loading).toBeFalsy();
        expect(result.current.data.reportId).toEqual("123");
    });
});
