import { renderHook, waitFor } from "@testing-library/react";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import { LookupTables, ValueSet } from "../config/endpoints/lookupTables";
import { AppWrapper } from "../utils/CustomRenderUtils";

import { useValueSetsTable } from "./UseValueSets";

describe("useValueSetsTable", () => {
    const renderWithAppWrapper = (tableName: LookupTables) =>
        renderHook(() => useValueSetsTable<ValueSet[]>(tableName), {
            wrapper: AppWrapper(),
        });

    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("returns expected data values when fetching table version", async () => {
        const { result } = renderWithAppWrapper(LookupTables.VALUE_SET);

        await waitFor(() => !!result.current.valueSetArray.length);
        const { name, system } = result.current.valueSetArray[0];
        expect(name).toEqual("sender_automation_value_set");
        expect(system).toEqual("LOCAL");
    });
});
