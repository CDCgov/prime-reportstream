import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

import { ApiValueSet, LookupTable, lookupTablesEndpoints } from "../config/endpoints/lookupTables";

const tableListUrl = lookupTablesEndpoints.getTableList.toDynamicUrl();
const tableDataUrl = lookupTablesEndpoints.getTableData.toDynamicUrl({
    tableName: "sender_automation_value_set",
});
const tableDataUrlAlt = lookupTablesEndpoints.getTableData.toDynamicUrl({
    tableName: "sender_automation_value_set_row",
});
const updateTableDataUrl = lookupTablesEndpoints.updateTable.toDynamicUrl({
    tableName: "any",
});
const activateTableDataUrl = lookupTablesEndpoints.activateTable.toDynamicUrl({
    version: "1",
    tableName: "any",
});

const lookupTables: LookupTable[] = [
    {
        lookupTableVersionId: 1,
        tableName: "sender_automation_value_set",
        tableVersion: 1,
        isActive: true,
        createdBy: "test@example.com",
        createdAt: "now",
        tableSha256Checksum: "checksum",
    },
    {
        lookupTableVersionId: 2,
        tableName: "sender_automation_value_set_row",
        tableVersion: 2,
        isActive: true,
        createdBy: "again@example.com",
        createdAt: "later",
        tableSha256Checksum: "checksum",
    },
];

const lookupTableData: ApiValueSet[] = [1, 2, 3].map((_i) => ({
    name: "sender_automation_value_set",
    created_by: "",
    created_at: "",
    system: "LOCAL",
    reference: "unused",
    referenceURL: "https://unused",
}));

const handlers = [
    http.get(tableListUrl, () => {
        return HttpResponse.json(lookupTables, { status: 200 });
    }),
    http.get(tableDataUrl, () => {
        return HttpResponse.json(lookupTableData, { status: 200 });
    }),
    http.get(tableDataUrlAlt, () => {
        return HttpResponse.json(lookupTableData, { status: 200 });
    }),
    http.post(updateTableDataUrl, () => {
        return HttpResponse.json(lookupTables[1], { status: 200 });
    }),
    http.put(activateTableDataUrl, () => {
        return HttpResponse.json(lookupTables[1], { status: 200 });
    }),
];

export const lookupTableServer = setupServer(...handlers);
