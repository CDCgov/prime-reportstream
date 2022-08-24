import { lookupTableApi, LookupTables, ValueSetRow } from "./LookupTableApi";

describe("Lookuptable API", () => {
    test("getTableList", () => {
        const endpoint = lookupTableApi.getTableList();
        expect(endpoint).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/lookuptables/list`,
            params: { showInactive: true },
            headers: {},
            responseType: "json",
        });
    });

    test("getTableData", () => {
        const endpointValueSet =
            lookupTableApi.getTableData<LookupTables.VALUE_SET>(
                1,
                LookupTables.VALUE_SET
            );
        expect(endpointValueSet).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/lookuptables/${LookupTables.VALUE_SET}/1/content`,
            headers: {},
            responseType: "json",
        });

        const endpointValueSetRow =
            lookupTableApi.getTableData<LookupTables.VALUE_SET_ROW>(
                1,
                LookupTables.VALUE_SET_ROW
            );
        expect(endpointValueSetRow).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/lookuptables/${LookupTables.VALUE_SET_ROW}/1/content`,
            headers: {},
            responseType: "json",
        });
    });

    test("saveTableData", () => {
        const endpoint = lookupTableApi.saveTableData<ValueSetRow>(
            LookupTables.VALUE_SET_ROW
        );
        expect(endpoint).toEqual({
            method: "POST",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/lookuptables/${LookupTables.VALUE_SET_ROW}`,
            headers: {},
            responseType: "json",
        });
    });

    test("activateTableData", () => {
        const endpoint = lookupTableApi.activateTableData<ValueSetRow>(
            2,
            LookupTables.VALUE_SET_ROW
        );
        expect(endpoint).toEqual({
            method: "PUT",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/lookuptables/${LookupTables.VALUE_SET_ROW}/2/activate`,
            headers: {},
            responseType: "json",
        });
    });
});
