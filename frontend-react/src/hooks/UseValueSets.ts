import { useCallback, useEffect, useMemo } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    LookupTable,
    ValueSet,
    ValueSetRow,
    LookupTables,
} from "../config/endpoints/lookupTables";
import { RSNetworkError } from "../utils/RSNetworkError";
import { useSessionContext } from "../contexts/SessionContext";

const { getTableData, getTableList, updateTable, activateTable } =
    lookupTablesEndpoints;

/*

  Helper function to find the table we want within the response for ALL tables
  Hopefully this will go away with the API refactor

*/
const findTableMetaByName = (
    tableName: string,
    tables: LookupTable[] = [],
): LookupTable | undefined => {
    if (!tables.length) {
        return undefined;
    }
    const filteredBody: LookupTable[] = tables.filter(
        (tv: LookupTable) => tv.tableName === tableName && tv.isActive,
    );

    if (!filteredBody.length) {
        return undefined;
    }
    return filteredBody.sort(
        (a: LookupTable, b: LookupTable) =>
            b["tableVersion"] - a["tableVersion"],
    )[0];
};

export type UseValueSetsTableResult = ReturnType<typeof useValueSetsTable>;

/*

  useValueSetsTable

  a useQuery based custom hook used to get value sets and value set rows (defined by passed dataTableName)

*/

export const useValueSetsTable = <T extends ValueSet[] | ValueSetRow[]>(
    dataTableName: string,
) => {
    const authorizedFetch = useAuthorizedFetch<T>();

    // create the function to use for fetching table data from the API
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(getTableData, {
                segments: {
                    tableName: dataTableName,
                },
            }),
        [authorizedFetch, dataTableName],
    );

    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    return useQuery({
        queryKey: [getTableData.queryKey, dataTableName],
        queryFn: memoizedDataFetch,
    });
};

export type UseValueSetsMetaResult = ReturnType<typeof useValueSetsMeta>;
/*

  useValueSetsMeta

  a useQuery based custom hook used to get metadata for a given value set

*/

export const useValueSetsMeta = (
    dataTableName: string = LookupTables.VALUE_SET,
) => {
    const authorizedFetch = useAuthorizedFetch<LookupTable[]>();
    const { rsconsole } = useSessionContext();

    // get all lookup tables in order to get metadata
    const { data: tableData, ...query } = useQuery({
        queryKey: [getTableList.queryKey],
        queryFn: () => authorizedFetch(getTableList),
    });

    const tableMeta = useMemo(
        () => findTableMetaByName(dataTableName, tableData),
        [dataTableName, tableData],
    );

    useEffect(() => {
        if (!tableMeta && tableData?.length) {
            rsconsole.info(
                "Unable to find metadata for lookup table: ",
                dataTableName,
            );
        }
    }, [tableMeta, tableData, dataTableName, rsconsole]);

    return { ...query, data: tableMeta };
};

/*

  Mutation Hooks

  */

interface UpdateValueSetOptions {
    data: ValueSetRow[];
    tableName: string;
}

interface ActivateValueSetOptions {
    tableVersion: number;
    tableName: string;
}

export type UseValueSetUpdateResult = ReturnType<typeof useValueSetUpdate>;
export const useValueSetUpdate = () => {
    const authorizedFetch = useAuthorizedFetch<LookupTable>();

    const updateValueSet = ({ data, tableName }: UpdateValueSetOptions) => {
        return authorizedFetch(updateTable, {
            segments: { tableName: tableName },
            data,
        });
    };

    // generic signature is defined here https://github.com/TanStack/query/blob/4690b585722d2b71d9b87a81cb139062d3e05c9c/packages/react-query/src/useMutation.ts#L66
    // <type of data returned, type of error returned, type of variables passed to mutate fn, type of context (?)>
    return useMutation<LookupTable, RSNetworkError, UpdateValueSetOptions>({
        mutationFn: updateValueSet,
    });
};

export type UseValueSetActivationResult = ReturnType<
    typeof useValueSetActivation
>;
export const useValueSetActivation = () => {
    const authorizedFetch = useAuthorizedFetch<LookupTable>();
    const activateValueSet = ({
        tableVersion,
        tableName,
    }: ActivateValueSetOptions) => {
        return authorizedFetch(activateTable, {
            segments: {
                tableName,
                version: `${tableVersion}`,
            },
        });
    };
    return useMutation<LookupTable, RSNetworkError, ActivateValueSetOptions>({
        mutationFn: activateValueSet,
    });
};
