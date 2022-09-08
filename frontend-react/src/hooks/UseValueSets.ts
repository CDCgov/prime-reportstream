import { useCallback } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    LookupTable,
    ValueSet,
    ValueSetRow,
    LookupTables,
} from "../config/endpoints/lookupTables";

const { getTableData, getTableList, updateTable, activateTable } =
    lookupTablesEndpoints;

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

  useValueSetsTable

  a useQuery based custom hook used to get value sets and value set rows (defined by passsed dataTableName)

*/

export const useValueSetsTable = <T extends ValueSet[] | ValueSetRow[]>(
    dataTableName: string
): {
    valueSetArray: T;
    error: any;
} => {
    const dataFetch = useAuthorizedFetch<T>();

    // create the function to use for fetching table data from the API
    const memoizedDataFetch = useCallback(
        () =>
            dataFetch(getTableData, {
                segments: {
                    tableName: dataTableName,
                },
            }),
        [dataFetch, dataTableName]
    );

    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    const { error, data: valueSetData } = useQuery<T>(
        [getTableData.queryKey, dataTableName],
        memoizedDataFetch
    );

    return { error, valueSetArray: valueSetData as T };
};

/*

  useValueSetsMeta

  a useQuery based custom hook used to get metadata for a given value set

*/

export const useValueSetsMeta = (
    dataTableName: string = LookupTables.VALUE_SET
): {
    valueSetMeta: LookupTable;
    error: any;
} => {
    const lookupTableFetch = useAuthorizedFetch<LookupTable[]>();

    // get all lookup tables in order to get metadata
    const { error, data: tableData } = useQuery<LookupTable[]>(
        [getTableList.queryKey],
        () => lookupTableFetch(getTableList)
    );

    const tableMeta = findTableMetaByName(tableData, dataTableName);

    return { error, valueSetMeta: tableMeta };
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

export const useValueSetUpdate = () => {
    const valueSetFetch = useAuthorizedFetch<LookupTable>();

    const updateValueSet = ({ data, tableName }: UpdateValueSetOptions) => {
        return valueSetFetch(updateTable, {
            segments: { tableName: tableName },
            data,
        });
    };

    // generic signature is defined here https://github.com/TanStack/query/blob/4690b585722d2b71d9b87a81cb139062d3e05c9c/packages/react-query/src/useMutation.ts#L66
    // <type of data returned, type of error returned, type of variables passed to mutate fn, type of context (?)>
    const mutation = useMutation<LookupTable, Error, UpdateValueSetOptions>(
        updateValueSet
    );
    return {
        saveData: mutation.mutateAsync,
        isSaving: mutation.isLoading,
        saveError: mutation.error,
    };
};

export const useValueSetActivation = () => {
    const valueSetFetch = useAuthorizedFetch<LookupTable>();
    const activateValueSet = ({
        tableVersion,
        tableName,
    }: ActivateValueSetOptions) => {
        return valueSetFetch(activateTable, {
            segments: {
                tableName,
                version: `${tableVersion}`,
            },
        });
    };
    const mutation = useMutation<LookupTable, Error, ActivateValueSetOptions>(
        activateValueSet
    );
    return {
        activateTable: mutation.mutateAsync,
        isActivating: mutation.isLoading,
        activationError: mutation.error,
    };
};
