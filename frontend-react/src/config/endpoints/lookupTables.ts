import { HTTPMethods, RSApiEndpoints, RSEndpoint } from ".";

// the shape used by the frontend client for value sets
export interface ValueSet {
    name: string;
    createdBy: string;
    createdAt: string;
    system: string;
}

// the shape sent down by the API for value sets
export interface ApiValueSet {
    name: string;
    created_by: string; // unused
    created_at: string; // unused
    system: string;
    reference: string; // unused
    referenceURL: string; // unused
}

export interface ValueSetRow {
    name: string;
    code: string;
    display: string;
    version: string;
}

export interface LookupTable {
    lookupTableVersionId: number;
    tableName: string;
    tableVersion: number;
    isActive: boolean;
    createdBy: string;
    createdAt: string;
    tableSha256Checksum: string;
}

export enum LookupTables {
    VALUE_SET = "sender_automation_value_set",
    VALUE_SET_ROW = "sender_automation_value_set_row",
}

/*
Lookup Table Endpoints

* getTableList -> returns metadata for all lookuptables
* getTableData -> given a table name, returns all data rows in the lookup table of that name, for the active version
* updateTable -> with a payload of ALL table rows, updates a table with new data, creating a new version of the table
* activateTable -> given a table name and version, activates the specified version of the lookup table of that name
*/

export const lookupTablesEndpoints: RSApiEndpoints = {
    getTableList: new RSEndpoint({
        path: "/lookuptables/list",
        method: HTTPMethods.GET,
        queryKey: "lookupTables",
    }),
    getTableData: new RSEndpoint({
        // notice the react-router style colon demarcated dynamic path segments
        path: "/lookuptables/:tableName/content",
        method: HTTPMethods.GET,
        queryKey: "lookupTable",
    }),
    updateTable: new RSEndpoint({
        path: "/lookuptables/:tableName",
        method: HTTPMethods.POST,
    }),
    activateTable: new RSEndpoint({
        path: "/lookuptables/:tableName/:version/activate",
        method: HTTPMethods.PUT,
    }),
};
