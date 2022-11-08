import { renderHook } from "@testing-library/react-hooks";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { deliveryServer } from "../../../__mocks__/DeliveriesMockServer";
import { QueryWrapper } from "../../../utils/CustomRenderUtils";

import {
    useReportsDetail,
    useOrgDeliveries,
    useReportsFacilities,
} from "./DeliveryHooks";
import { FilterManagerDefaults } from "../../filters/UseFilterManager";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "batchReadyAt",
        locally: true,
    },
    pageDefaults: {
        size: 5,
    },
};

describe("DeliveryHooks", () => {
    beforeAll(() => deliveryServer.listen());
    afterEach(() => deliveryServer.resetHandlers());
    afterAll(() => deliveryServer.close());
    test("useReportsList", async () => {
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
            () =>
                useOrgDeliveries(
                    "testOrg",
                    "testService",
                    filterManagerDefaults
                ),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.fetchResults).toHaveLength(3);
        expect(result.current.filterManager).toEqual(filterManagerDefaults);
    });
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
        });
        const { result, waitForNextUpdate } = renderHook(
            () => useReportsDetail("123"),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.reportDetail?.reportId).toEqual("123");
    });
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
            () => useReportsFacilities("123"),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.reportFacilities?.length).toEqual(2);
    });
});
