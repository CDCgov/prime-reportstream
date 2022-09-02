import { useCallback } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    LookupTable,
    ValueSet,
    ValueSetRow,
} from "../config/endpoints/lookupTables";

export interface TableAttributes {
    version: number;
    createdAt?: string;
    createdBy?: string;
}

const { getTableData, getTableList, updateTable, activateTable } =
    lookupTablesEndpoints;

/*

  Helper function to find the table we want within the response for ALL tables
  Hopefully this will go away with the API refactor

*/
const findTableByName = (
    tables: LookupTable[],
    tableName: string
): TableAttributes => {
    const filteredBody: LookupTable[] = tables.filter(
        (tv: LookupTable) => tv.tableName === tableName && tv.isActive
    );

    if (!filteredBody.length) {
        throw new Error(`Table '${tableName}' was not found!`);
    }
    const table: LookupTable = filteredBody.sort(
        (a: LookupTable, b: LookupTable) =>
            b["tableVersion"] - a["tableVersion"]
    )[0];

    const { tableVersion, createdAt, createdBy } = table;
    if (tableVersion === undefined) {
        throw new Error(`No version of table '${tableName}' was found!`);
    }

    return {
        version: tableVersion,
        createdAt,
        createdBy,
    };
};

/*

  useQuery function

  hook used to get value sets and value set rows (defined by passsed dataTableName)

*/

export const useValueSetsTable = <T extends ValueSet[] | ValueSetRow[]>(
    dataTableName: string,
    suppliedVersion?: number
): {
    valueSetArray: T;
    error: any;
} => {
    let versionData: Partial<TableAttributes> | null;
    let error;

    // multiple calls to the hook for different types is awkward but
    // will be less awkward once resources are introduced, as those will be passed in
    // OR we could move to a world where this hook just returns the generator function, and
    // we call the generator function within our component called hooks to type the functions?
    const lookupTableFetch = useAuthorizedFetch<LookupTable[]>();
    const dataFetch = useAuthorizedFetch<T>();

    // get all lookup tables
    const { error: tableError, data: tableData } = useQuery<LookupTable[]>(
        [getTableList.queryKey],
        () => lookupTableFetch(getTableList),
        { enabled: !suppliedVersion } // only if version was not already passed in
    );

    // handle complexity around passed version vs. incomplete useQuery vs. success
    if (suppliedVersion) {
        versionData = {
            version: suppliedVersion,
        };
    } else if (!tableData) {
        versionData = null;
    } else {
        try {
            versionData = findTableByName(tableData, dataTableName);
        } catch (e) {
            error = e;
            versionData = null;
        }
    }

    // create the function to use for fetching table data from the API
    const memoizedDataFetch = useCallback(
        () =>
            dataFetch(getTableData, {
                segments: {
                    tableName: dataTableName,
                    version: `${versionData!!.version!}`, // number to string,
                },
            }),
        [dataFetch, versionData, dataTableName]
    );

    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    const { error: valueSetError, data: valueSetData } = useQuery<T>(
        [getTableData.queryKey, versionData?.version, dataTableName],
        memoizedDataFetch,
        { enabled: !!versionData?.version }
    );

    // the logic here is that the value set request should not go out if there is an error
    // in the version call, so an error in the version call should preempt an error in the
    // data call
    error = error || tableError || valueSetError || null;
    const valueSetArray = valueSetData
        ? valueSetData.map((el) => ({ ...el, ...versionData }))
        : [];

    return { error, valueSetArray: valueSetArray as T };
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
