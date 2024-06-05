import { waitFor } from "@testing-library/react";

import useValueSetsTable from "./UseValueSetsTable";
import { lookupTableServer } from "../../../../__mockServers__/LookupTableMockServer";
import {
    LookupTables,
    ValueSet,
} from "../../../../config/endpoints/lookupTables";
import { AppWrapper, renderHook } from "../../../../utils/CustomRenderUtils";

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

        await waitFor(() =>
            expect(result.current.data?.length).toBeGreaterThan(0),
        );
        const { name, system } = result.current.data[0];
        expect(name).toEqual("sender_automation_value_set");
        expect(system).toEqual("LOCAL");
    });
});
