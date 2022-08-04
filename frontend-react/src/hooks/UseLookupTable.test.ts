import { renderHook } from "@testing-library/react-hooks";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import { LookupTables, ValueSet } from "../network/api/LookupTableApi";

import useLookupTable, {
    getLatestData,
    getLatestVersion,
    getSenderAutomationData,
} from "./UseLookupTable";

describe("useLookupTable and related helper functions", () => {
    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    describe("getLatestVersion", () => {
        test("getLatestVersion returns expected version and timestamps", async () => {
            const result = await getLatestVersion(LookupTables.VALUE_SET);
            expect(result).toBeTruthy();
            if (!result) return; // I don't like this, as the case is handled in the test above but shrug emoji - DWS
            const { version, createdAt, createdBy } = result;
            expect(version).toEqual(2);
            expect(createdAt).toEqual("now");
            expect(createdBy).toEqual("test@example.com");
        });

        test("getLatestVersion throws when table doesn't exist", async () => {
            try {
                await getLatestVersion(LookupTables.VALUE_SET_ROW);
                expect(true).toBe(false);
            } catch (e: any) {
                expect(e.message).toEqual(
                    `Table 'sender_automation_value_set_row' was not found!`
                );
            }
        });
    });

    test("getLatestData returns expected data", async () => {
        const data = (await getLatestData<ValueSet>(
            2,
            LookupTables.VALUE_SET
        )) as ValueSet[];
        expect(data[0].name).toEqual(LookupTables.VALUE_SET);
    });

    test("getSenderAutomationData returns expected number of rows", async () => {
        const data = await getSenderAutomationData<ValueSet>(
            LookupTables.VALUE_SET
        );
        expect(data.length).toEqual(3);
    });

    test("useLookupTable hook returns expected number of rows", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useLookupTable<ValueSet>(LookupTables.VALUE_SET)
        );
        await waitForNextUpdate();
        expect(result.current.valueSetArray.length).toEqual(3);
    });
});
