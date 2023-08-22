import { useCallback } from "react";
import { useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    LookupTable,
    ValueSet,
    ValueSetRow,
    LookupTables,
} from "../config/endpoints/lookupTables";
import { RSNetworkError } from "../utils/RSNetworkError";

const { getTableData, getTableList, updateTable, activateTable } =
    lookupTablesEndpoints;

/*

  Helper function to find the table we want within the response for ALL tables
  Hopefully this will go away with the API refactor

*/
const findTableMetaByName = (
    tables: LookupTable[] = [],
    tableName: string,
): LookupTable => {
    if (!tables.length) {
        return {} as LookupTable;
    }
    const filteredBody: LookupTable[] = tables.filter(
        (tv: LookupTable) => tv.tableName === tableName && tv.isActive,
    );

    if (!filteredBody.length) {
        console.info("Unable to find metadata for lookup table: ", tableName);
        return {} as LookupTable;
    }
    return filteredBody.sort(
        (a: LookupTable, b: LookupTable) =>
            b["tableVersion"] - a["tableVersion"],
    )[0];
};

/*

  useValueSetsTable

  a useQuery based custom hook used to get value sets and value set rows (defined by passed dataTableName)

*/
export interface ValueSetsTableResponse<T> {
    valueSetArray: T;
}
export const useValueSetsTable = <T extends ValueSet[] | ValueSetRow[]>(
    dataTableName: string,
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
        [authorizedFetch, dataTableName],
    );

    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    const { data: valueSetData } = rsUseQuery(
        [getTableData.queryKey, dataTableName],
        memoizedDataFetch,
    );

    return { valueSetArray: valueSetData as T };
};

/*

  useValueSetsMeta

  a useQuery based custom hook used to get metadata for a given value set

*/
export interface ValueSetsMetaResponse {
    valueSetMeta: LookupTable;
}
export const useValueSetsMeta = (
    dataTableName: string = LookupTables.VALUE_SET,
): ValueSetsMetaResponse => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<LookupTable[]>();

    // get all lookup tables in order to get metadata
    const { data: tableData } = rsUseQuery([getTableList.queryKey], () =>
        authorizedFetch(getTableList),
    );

    const tableMeta = findTableMetaByName(tableData, dataTableName);

    return { valueSetMeta: tableMeta };
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
    const { authorizedFetch } = useAuthorizedFetch<LookupTable>();

    const updateValueSet = ({ data, tableName }: UpdateValueSetOptions) => {
        return authorizedFetch(updateTable, {
            segments: { tableName: tableName },
            data,
        });
    };

    // generic signature is defined here https://github.com/TanStack/query/blob/4690b585722d2b71d9b87a81cb139062d3e05c9c/packages/react-query/src/useMutation.ts#L66
    // <type of data returned, type of error returned, type of variables passed to mutate fn, type of context (?)>
    const mutation = useMutation<
        LookupTable,
        RSNetworkError,
        UpdateValueSetOptions
    >(updateValueSet);
    return {
        saveData: mutation.mutateAsync,
        isSaving: mutation.isLoading,
        saveError: mutation.error,
    };
};

export const useValueSetActivation = () => {
    const { authorizedFetch } = useAuthorizedFetch<LookupTable>();
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
    const mutation = useMutation<
        LookupTable,
        RSNetworkError,
        ActivateValueSetOptions
    >(activateValueSet);
    return {
        activateTable: mutation.mutateAsync,
        isActivating: mutation.isLoading,
        activationError: mutation.error,
    };
};
