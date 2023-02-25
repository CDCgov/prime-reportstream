import { useCallback } from "react";

import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    ValueSet,
    ValueSetRow,
} from "../config/endpoints/lookupTables";

const { getTableData } = lookupTablesEndpoints;

/*

  useValueSetsTable

  a useQuery based custom hook used to get value sets and value set rows (defined by passsed dataTableName)

*/
export interface ValueSetsTableResponse<T> {
    valueSetArray: T;
}
export const useValueSetsTable = <T extends ValueSet[] | ValueSetRow[]>(
    dataTableName: string
): ValueSetsTableResponse<T> => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<T>();

    // create the function to use for fetching table data from the API
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getTableData, {
                segments: {
                    tableName: dataTableName,
                },
            }),
        [authorizedFetch, dataTableName]
    );

    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    const { data: valueSetData } = rsUseQuery(
        [getTableData.queryKey, dataTableName],
        memoizedDataFetch
    );

    return { valueSetArray: valueSetData as T };
};
