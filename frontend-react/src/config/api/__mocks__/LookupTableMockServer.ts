import { rest } from "msw";
import { setupServer } from "msw/node";

import { lookupTablesEndpoints } from "../lookupTables";

const tableListUrl = lookupTablesEndpoints.lookupTables.toDynamicUrl();
const tableDataUrl = lookupTablesEndpoints.lookupTableContent.toDynamicUrl({
    tableName: "sender_automation_value_set",
});
const tableDataUrlAlt = lookupTablesEndpoints.lookupTableContent.toDynamicUrl({
    tableName: "sender_automation_value_set_row",
});
const updateTableDataUrl = lookupTablesEndpoints.updateLookupTable.toDynamicUrl(
    {
        tableName: "any",
    }
);
const activateTableDataUrl =
    lookupTablesEndpoints.activateLookupTableVersion.toDynamicUrl({
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
    created_by: "", // eslint-disable-line camelcase
    created_at: "", // eslint-disable-line camelcase
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
    rest.get(tableDataUrlAlt, (_req, res, ctx) => {
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
