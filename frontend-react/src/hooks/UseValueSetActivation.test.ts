import { act, renderHook } from "@testing-library/react";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import { AppWrapper } from "../utils/CustomRenderUtils";

import { useValueSetActivation } from "./UseValueSets";

describe("useValueSetActivation", () => {
    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    const renderWithAppWrapper = () =>
        renderHook(() => useValueSetActivation(), {
            wrapper: AppWrapper(),
        });

    test("returns trigger and loading indicator", () => {
        const { result } = renderWithAppWrapper();
        const { activateTable, isActivating, activationError } = result.current;
        expect(isActivating).toEqual(false);
        expect(activateTable).toBeInstanceOf(Function);
        expect(activationError).toBeNull();
    });

    test("mutation trigger returns expected values and tracks loading state", async () => {
        const { result } = renderWithAppWrapper();
        const { activateTable, isActivating } = result.current;
        expect(isActivating).toEqual(false);

        let activateResult;
        await act(async () => {
            const activationPromise = activateTable({
                tableVersion: 1,
                tableName: "any",
            });
            activateResult = await activationPromise;
        });

        expect(activateResult).toEqual({
            lookupTableVersionId: 2,
            tableName: "sender_automation_value_set_row",
            tableVersion: 2,
            isActive: true,
            createdBy: "again@example.com",
            createdAt: "later",
            tableSha256Checksum: "checksum",
        });
    });
});
