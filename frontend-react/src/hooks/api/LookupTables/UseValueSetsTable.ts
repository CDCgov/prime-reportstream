import { lookupTablesEndpoints } from "../../../config/api/lookupTables";
import { useRSQuery } from "../UseRSQuery";

/*

  useValueSetsTable

  a useQuery based custom hook used to get value sets and value set rows (defined by passsed dataTableName)

*/
export function useValueSetsTable<
    TQueryFnData extends Awaited<
        ReturnType<typeof lookupTablesEndpoints.lookupTableContent.queryFn>
    >
>(dataTableName: string) {
    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    return useRSQuery<
        typeof lookupTablesEndpoints.lookupTableContent,
        TQueryFnData extends any ? Extract<TQueryFnData, TQueryFnData> : never
    >(lookupTablesEndpoints.lookupTableContent, {
        segments: {
            tableName: dataTableName,
        },
    });
}
