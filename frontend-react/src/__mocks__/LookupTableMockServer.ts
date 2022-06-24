import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    lookupTableApi,
    LookupTable,
    ValueSet,
} from "../network/api/LookupTableApi";

const tableList = lookupTableApi.getTableList();
const tableData = lookupTableApi.getTableData<LookupTable>(
    2,
    "sender_automation_value_set"
);

const lookupTables: LookupTable[] = [1, 2, 3].map((i) => ({
    lookupTableVersionId: i,
    tableName: "sender_automation_value_set",
    tableVersion: i,
    isActive: i !== 3,
    createdBy: "test@example.com",
    createdAt: "now",
    tableSha256Checksum: "checksum",
})) as LookupTable[];

const lookupTableData: ValueSet[] = [1, 2, 3].map((i) => ({
    name: "sender_automation_value_set",
    createdBy: `test${i}@example.com`,
    createdAt: "now",
    system: "LOCAL",
})) as ValueSet[];

const handlers = [
    rest.get(tableList.url, (_req, res, ctx) => {
        return res(ctx.json(lookupTables), ctx.status(200));
    }),
    rest.get(tableData.url, (_req, res, ctx) => {
        return res(ctx.json(lookupTableData), ctx.status(200));
    }),
];

export const lookupTableServer = setupServer(...handlers);
