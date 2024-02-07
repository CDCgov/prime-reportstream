import { act, waitFor } from "@testing-library/react";

import { useOrganizationReceivers } from "./UseOrganizationReceivers";
import {
    sortAndFilterInactiveServices,
    useOrganizationReceiversFeed,
} from "./UseOrganizationReceiversFeed";
import {
    dummyActiveReceiver,
    dummyReceivers,
} from "../__mocks__/OrganizationMockServer";
import { renderHook } from "../utils/CustomRenderUtils";

jest.mock<typeof import("./UseOrganizationReceivers")>(
    "./UseOrganizationReceivers",
    () => ({
        ...jest.requireActual("./UseOrganizationReceivers"),
        useOrganizationReceivers: jest.fn(),
    }),
);

const mockUseOrganizationReceivers = jest.mocked(useOrganizationReceivers);

describe("useOrganizationReceiversFeed", () => {
    function setMockUseOrganizationReceiversResult(
        mock: Partial<ReturnType<typeof useOrganizationReceivers>>,
    ) {
        return mockUseOrganizationReceivers.mockImplementation(
            () => mock as any,
        );
    }

    test("returns correct result", async () => {
        setMockUseOrganizationReceiversResult({
            data: dummyReceivers,
            isLoading: false,
        });
        const { result } = renderHook(() => useOrganizationReceiversFeed());
        await waitFor(() =>
            expect(result.current.data).toEqual(
                sortAndFilterInactiveServices(dummyReceivers),
            ),
        );
        expect(result.current.setActiveService).toBeDefined();
        await waitFor(() =>
            expect(result.current.activeService).toEqual(dummyActiveReceiver),
        );
    });

    test("setActiveService sets an active receiver", async () => {
        setMockUseOrganizationReceiversResult({ data: dummyReceivers });
        const { result } = renderHook(() => useOrganizationReceiversFeed());
        await waitFor(() => expect(result.current.activeService).toBeDefined());
        expect(result.current.activeService).toEqual({
            name: "abc-1",
            organizationName: "testOrg",
        });
        expect(result.current.data).toBeDefined();
        act(() => result.current.setActiveService(result.current.data![1]));
        expect(result.current.activeService).toEqual({
            name: "elr-0",
            organizationName: "testOrg",
        });
    });
});
