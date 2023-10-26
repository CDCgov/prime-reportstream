import { waitFor } from "@testing-library/react";

import { mockSessionContentReturnValue } from "../../../contexts/__mocks__/SessionContext";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { AppWrapper, renderHook } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";

import { useReportsFacilities } from "./DeliveryHooks";

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportFacilities", async () => {
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
        const { result } = renderHook(() => useReportsFacilities("123"), {
            wrapper: AppWrapper(),
        });
        await waitFor(() =>
            expect(result.current.reportFacilities?.length).toEqual(2),
        );
    });
});
