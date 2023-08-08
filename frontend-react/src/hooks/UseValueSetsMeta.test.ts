import { renderHook, waitFor } from "@testing-library/react";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import { LookupTables } from "../config/endpoints/lookupTables";
import { AppWrapper } from "../utils/CustomRenderUtils";

import { useValueSetsMeta } from "./UseValueSets";

describe("useValueSetsMeta", () => {
    const renderWithAppWrapper = (tableName?: LookupTables) =>
        renderHook(() => useValueSetsMeta(tableName), {
            wrapper: AppWrapper(),
        });

    beforeAll(() =>
        lookupTableServer.listen({
            onUnhandledRequest: (req) => {
                console.error("unhandled");
                throw new Error("unhandled " + req.url);
            },
        }),
    );
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("returns expected meta values", async () => {
        const { result } = renderWithAppWrapper();
        await waitFor(() =>
            expect(result.current.valueSetMeta.createdAt).toBeDefined(),
        );
        const { createdAt, createdBy } = result.current.valueSetMeta;
        expect(createdAt).toEqual("now");
        expect(createdBy).toEqual("test@example.com");
    });

    test("returns expected meta values when passed an optional table name", async () => {
        const { result } = renderWithAppWrapper(LookupTables.VALUE_SET_ROW);

        await waitFor(() =>
            expect(result.current.valueSetMeta.createdAt).toBeDefined(),
        );
        const { createdAt, createdBy } = result.current.valueSetMeta;
        expect(createdAt).toEqual("later");
        expect(createdBy).toEqual("again@example.com");
    });

    test("returns empty metadata when the passed table name doesn't exist in returned list of tables", async () => {
        const { result } = renderWithAppWrapper();
        await waitFor(() => expect(result.current.valueSetMeta).toBeDefined());
        expect(result.current.valueSetMeta).toEqual({});
    });
});
