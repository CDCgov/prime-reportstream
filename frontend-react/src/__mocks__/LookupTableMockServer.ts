import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    lookupTablesEndpoints,
    LookupTable,
    ApiValueSet,
} from "../config/endpoints/lookupTables";

const tableListUrl = lookupTablesEndpoints.getTableList.toDynamicUrl();
const tableDataUrl = lookupTablesEndpoints.getTableData.toDynamicUrl({
    version: "2",
    tableName: "sender_automation_value_set",
});
const tableDataAlternateUrl = lookupTablesEndpoints.getTableData.toDynamicUrl({
    version: "3",
    tableName: "sender_automation_value_set",
});
const updateTableDataUrl = lookupTablesEndpoints.updateTable.toDynamicUrl({
    tableName: "any",
});
const activateTableDataUrl = lookupTablesEndpoints.activateTable.toDynamicUrl({
    version: "1",
    tableName: "any",
});

const lookupTables: LookupTable[] = [1, 2, 3].map((i) => ({
    lookupTableVersionId: i,
    tableName: "sender_automation_value_set",
    tableVersion: i,
    isActive: i !== 3,
    createdBy: "test@example.com",
    createdAt: "now",
    tableSha256Checksum: "checksum",
})) as LookupTable[];

const lookupTableData: ApiValueSet[] = [1, 2, 3].map((_i) => ({
    name: "sender_automation_value_set",
    created_by: "",
    created_at: "",
    system: "LOCAL",
    reference: "unused",
    referenceURL: "https://unused",
}));

const handlers = [
    rest.get(tableListUrl, (_req, res, ctx) => {
        return res(ctx.json(lookupTables), ctx.status(200));
    }),
    rest.get(tableDataUrl, (_req, res, ctx) => {
        return res(ctx.json(lookupTableData), ctx.status(200));
    }),
    rest.get(tableDataAlternateUrl, (_req, res, ctx) => {
        return res(ctx.json(lookupTableData), ctx.status(200));
    }),
    rest.post(updateTableDataUrl, (_req, res, ctx) => {
        return res(ctx.json(lookupTables[1]), ctx.status(200));
    }),
    rest.put(activateTableDataUrl, (_req, res, ctx) => {
        return res(ctx.json(lookupTables[1]), ctx.status(200));
    }),
];

export const lookupTableServer = setupServer(...handlers);
