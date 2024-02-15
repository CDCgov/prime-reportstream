import { waitFor } from "@testing-library/react";

import { useReportsDetail } from "./DeliveryHooks";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { AppWrapper, renderHook } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = jest.requireMock(
    "../../../contexts/Session/useSessionContext",
);

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportDetail", async () => {
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
        const { result } = renderHook(() => useReportsDetail("123"), {
            wrapper: AppWrapper(),
        });
        await waitFor(() =>
            expect(result.current.data?.reportId).toEqual("123"),
        );
    });
});
