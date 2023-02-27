import { renderHook } from "@testing-library/react-hooks";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { QueryWrapper } from "../../../utils/CustomRenderUtils";

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
        });
        const { result, waitForNextUpdate } = renderHook(
            () => useReportsDetail("123"),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.reportDetail?.reportId).toEqual("123");
    });
});
