import { renderHook } from "@testing-library/react-hooks";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../config/api/__mocks__/DeliveriesMockServer";
import { QueryWrapper } from "../../../utils/CustomRenderUtils";

import { useReportFacilities } from "./UseReportFacilities";

describe(useReportFacilities.name, () => {
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
        });
        const { result, waitForNextUpdate } = renderHook(
            () => useReportFacilities("123"),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.data?.length).toEqual(2);
    });
});
