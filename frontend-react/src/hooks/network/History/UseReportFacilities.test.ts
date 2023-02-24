import { renderHook } from "@testing-library/react-hooks";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { QueryWrapper } from "../../../utils/CustomRenderUtils";

import { useReportsFacilities } from "./DeliveryHooks";

describe("useReportsList", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportFacilities", async () => {
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
            () => useReportsFacilities("123"),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.reportFacilities?.length).toEqual(2);
    });
});
