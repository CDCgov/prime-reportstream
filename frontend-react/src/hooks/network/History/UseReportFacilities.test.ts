import { renderHook, waitFor } from "@testing-library/react";

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
        const { result } = renderHook(() => useReportsFacilities("123"), {
            wrapper: QueryWrapper(),
        });
        await waitFor(() =>
            expect(result.current.reportFacilities?.length).toEqual(2)
        );
    });
});
