import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";

import { lookupTablesEndpoints, ValueSet, ValueSetRow } from "../../../../config/endpoints/lookupTables";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { getTableData } = lookupTablesEndpoints;

export type UseValueSetsTableResult = ReturnType<typeof useValueSetsTable>;

/** useValueSetsTable
 * a useQuery based custom hook used to get value sets and value set rows (defined by passed dataTableName)
 */
const useValueSetsTable = <T extends ValueSet[] | ValueSetRow[]>(dataTableName: string) => {
    const { authorizedFetch } = useSessionContext();

    // create the function to use for fetching table data from the API
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch<T>(
                {
                    segments: {
                        tableName: dataTableName,
                    },
                },
                getTableData,
            ),
        [authorizedFetch, dataTableName],
    );

    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    return useSuspenseQuery({
        queryKey: [getTableData.queryKey, dataTableName],
        queryFn: memoizedDataFetch,
    });
};

export default useValueSetsTable;
