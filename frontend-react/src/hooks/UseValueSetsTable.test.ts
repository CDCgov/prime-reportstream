import { renderHook, waitFor } from "@testing-library/react";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import { LookupTables, ValueSet } from "../config/endpoints/lookupTables";
import { QueryWrapper } from "../utils/CustomRenderUtils";

import { useValueSetsTable } from "./UseValueSets";

describe("useValueSetsTable", () => {
    const renderWithQueryWrapper = (tableName: LookupTables) =>
        renderHook(() => useValueSetsTable<ValueSet[]>(tableName), {
            wrapper: QueryWrapper(),
        });

    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("returns expected data values when fetching table version", async () => {
        const { result } = renderWithQueryWrapper(LookupTables.VALUE_SET);

        await waitFor(() => !!result.current.valueSetArray.length);
        const { name, system } = result.current.valueSetArray[0];
        expect(name).toEqual("sender_automation_value_set");
        expect(system).toEqual("LOCAL");
    });
});
