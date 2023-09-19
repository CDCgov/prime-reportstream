import { renderHook, waitFor } from "@testing-library/react";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { AppWrapper } from "../../../utils/CustomRenderUtils";

import { useReportsDetail } from "./DeliveryHooks";

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportDetail", async () => {
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
        const { result } = renderHook(() => useReportsDetail("123"), {
            wrapper: AppWrapper(),
        });
        await waitFor(() =>
            expect(result.current.reportDetail?.reportId).toEqual("123"),
        );
    });
});
