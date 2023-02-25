import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    LookupTable,
    LookupTables,
} from "../config/endpoints/lookupTables";

const { getTableList } = lookupTablesEndpoints;

/*

  Helper function to find the table we want within the response for ALL tables
  Hopefully this will go away with the API refactor

*/
const findTableMetaByName = (
    tables: LookupTable[] = [],
    tableName: string
): LookupTable => {
    if (!tables.length) {
        return {} as LookupTable;
    }
    const filteredBody: LookupTable[] = tables.filter(
        (tv: LookupTable) => tv.tableName === tableName && tv.isActive
    );

    if (!filteredBody.length) {
        console.log("Unable to find metadata for lookup table: ", tableName);
        return {} as LookupTable;
    }
    return filteredBody.sort(
        (a: LookupTable, b: LookupTable) =>
            b["tableVersion"] - a["tableVersion"]
    )[0];
};

/*

  useValueSetsMeta

  a useQuery based custom hook used to get metadata for a given value set

*/
export interface ValueSetsMetaResponse {
    valueSetMeta: LookupTable;
}
export const useValueSetsMeta = (
    dataTableName: string = LookupTables.VALUE_SET
): ValueSetsMetaResponse => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<LookupTable[]>();

    // get all lookup tables in order to get metadata
    const { data: tableData } = rsUseQuery([getTableList.queryKey], () =>
        authorizedFetch(getTableList)
    );

    const tableMeta = findTableMetaByName(tableData, dataTableName);

    return { valueSetMeta: tableMeta };
};
