import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    lookupTableApi,
    LookupTable,
    ApiValueSet,
} from "../network/api/LookupTableApi";

// TODO: refactor this to use resource based configs????
const tableList = lookupTableApi.getTableList();
const tableData = lookupTableApi.getTableData<LookupTable>(
    2,
    "sender_automation_value_set"
);
const tableDataAlternate = lookupTableApi.getTableData<LookupTable>(
    3,
    "sender_automation_value_set"
);

const updateTableData = lookupTableApi.saveTableData("any");

const activateTableData = lookupTableApi.activateTableData(1, "any");

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
    referenceURL: "http://unused",
}));

const handlers = [
    rest.get(tableList.url, (_req, res, ctx) => {
        return res(ctx.json(lookupTables), ctx.status(200));
    }),
    rest.get(tableData.url, (_req, res, ctx) => {
        return res(ctx.json(lookupTableData), ctx.status(200));
    }),
    rest.get(tableDataAlternate.url, (_req, res, ctx) => {
        return res(ctx.json(lookupTableData), ctx.status(200));
    }),
    rest.post(updateTableData.url, (_req, res, ctx) => {
        return res(ctx.json(lookupTables[1]), ctx.status(200));
    }),
    rest.put(activateTableData.url, (_req, res, ctx) => {
        return res(ctx.json(lookupTables[1]), ctx.status(200));
    }),
];

export const lookupTableServer = setupServer(...handlers);
