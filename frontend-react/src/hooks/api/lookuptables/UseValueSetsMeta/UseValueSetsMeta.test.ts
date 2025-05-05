import { waitFor } from "@testing-library/react";

import useValueSetsMeta from "./UseValueSetsMeta";
import { lookupTableServer } from "../../../../__mockServers__/LookupTableMockServer";
import { LookupTables } from "../../../../config/endpoints/lookupTables";
import { renderHook } from "../../../../utils/CustomRenderUtils";

describe("useValueSetsMeta", () => {
    const renderWithAppWrapper = (tableName?: LookupTables) => renderHook(() => useValueSetsMeta(tableName));

    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("returns expected meta values", async () => {
        const { result } = renderWithAppWrapper();
        await waitFor(() => expect(result.current.data?.createdAt).toBeDefined());
        const { createdAt, createdBy } = result.current.data!;
        expect(createdAt).toEqual("now");
        expect(createdBy).toEqual("test@example.com");
    });

    test("returns expected meta values when passed an optional table name", async () => {
        const { result } = renderWithAppWrapper(LookupTables.VALUE_SET_ROW);

        await waitFor(() => expect(result.current.data?.createdAt).toBeDefined());
        const { createdAt, createdBy } = result.current.data!;
        expect(createdAt).toEqual("later");
        expect(createdBy).toEqual("again@example.com");
    });

    test("returns empty metadata when the passed table name doesn't exist in returned list of tables", async () => {
        const { result } = renderWithAppWrapper("invalid" as LookupTables);
        await waitFor(() => expect(result.current.data).toBeUndefined());
    });
});
