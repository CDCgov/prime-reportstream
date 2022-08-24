import { rest } from "msw";
import { renderHook } from "@testing-library/react-hooks";
import { QueryClient } from "@tanstack/react-query";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import {
    LookupTables,
    ValueSet,
    lookupTableApi,
    LookupTable,
} from "../network/api/LookupTableApi";
import { QueryWrapper } from "../utils/CustomRenderUtils";

import { useValueSetsTable } from "./UseValueSets";

describe("useValueSetsTable", () => {
    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("returns expected values when fetching table version", async () => {
        const { result, waitFor } = renderHook(
            () => useValueSetsTable<ValueSet>(LookupTables.VALUE_SET),
            { wrapper: QueryWrapper() }
        );
        await waitFor(() => !!result.current.valueSetArray.length);
        const { name, system, createdAt, createdBy } =
            result.current.valueSetArray[0];
        expect(name).toEqual("sender_automation_value_set");
        expect(createdAt).toEqual("now");
        expect(createdBy).toEqual("test@example.com");
        expect(system).toEqual("LOCAL");
    });

    test("returns expected values when using supplied table version", async () => {
        const { result, waitFor } = renderHook(
            () => useValueSetsTable<ValueSet>(LookupTables.VALUE_SET, 3),
            { wrapper: QueryWrapper() }
        );
        await waitFor(() => !!result.current.valueSetArray.length);
        const { name, system, createdAt, createdBy } =
            result.current.valueSetArray[0];
        expect(name).toEqual("sender_automation_value_set");
        expect(createdAt).toEqual(undefined); // not expecting this data to be present in this case
        expect(createdBy).toEqual(undefined); // not expecting this data to be present in this case
        expect(system).toEqual("LOCAL");
    });

    test("returns error when the passed table name doesn't exist in returned list of tables", async () => {
        const { result, waitFor } = renderHook(
            () => useValueSetsTable(LookupTables.VALUE_SET_ROW),
            { wrapper: QueryWrapper() }
        );
        await waitFor(() => !!result.current.error);
        expect(result.current.error.message).toEqual(
            `Table 'sender_automation_value_set_row' was not found!`
        );
    });

    test("returns error when table list call errors", async () => {
        lookupTableServer.use(
            rest.get(lookupTableApi.getTableList().url, (_req, res, ctx) => {
                return res.once(ctx.json([]), ctx.status(400));
            })
        );
        const { result, waitForNextUpdate } = renderHook(
            () => useValueSetsTable(LookupTables.VALUE_SET),
            {
                wrapper: QueryWrapper(
                    new QueryClient({
                        defaultOptions: { queries: { retry: false } },
                    })
                ),
            }
        );
        await waitForNextUpdate();
        expect(result.current.error.message).toEqual(
            "Request failed with status code 400"
        );
    });

    test("returns error when table data call errors", async () => {
        lookupTableServer.use(
            rest.get(
                lookupTableApi.getTableData<LookupTable>(
                    2,
                    "sender_automation_value_set"
                ).url,
                (_req, res, ctx) => {
                    return res.once(ctx.json([]), ctx.status(400));
                }
            )
        );
        const { result, waitFor } = renderHook(
            () => useValueSetsTable(LookupTables.VALUE_SET),
            {
                wrapper: QueryWrapper(
                    new QueryClient({
                        defaultOptions: { queries: { retry: false } },
                    })
                ),
            }
        );
        await waitFor(() => result.current.error);
        expect(result.current.error.message).toEqual(
            "Request failed with status code 400"
        );
    });
});
