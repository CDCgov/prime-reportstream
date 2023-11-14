import { waitFor } from "@testing-library/react";

import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { MemberType } from "../../../utils/OrganizationUtils";

import { useReportsDetail } from "./DeliveryHooks";

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportDetail", async () => {
        const { result } = renderHook(() => useReportsDetail("123"), {
            providers: {
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
        await waitFor(() =>
            expect(result.current.data?.reportId).toEqual("123"),
        );
    });
});
