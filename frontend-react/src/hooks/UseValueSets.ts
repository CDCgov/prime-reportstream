import axios from "axios";
import { useQuery, useMutation } from "@tanstack/react-query";

import {
    LookupTable,
    lookupTableApi,
    LookupTables,
    ValueSet,
    ValueSetRow,
} from "../network/api/LookupTableApi";

export interface TableAttributes {
    version: number;
    createdAt?: string;
    createdBy?: string;
}

/*

  Fetch / Mutate functions and helpersa

*/

/*
TO DISCUSS: we could set the pattern to be something like this or
we could reference query keys within the function https://stackoverflow.com/a/68111112/5500298

Have not tested, but I feel like to get the full value of react-query's refetching behavior we may want to go with the last option.
When react-query refetches it will just re-run the function we passed to it, this would not take into account changes in variable dependencies
unless we bake those into function itself?

Note that I ran into type errors when trying to use higher order functions to avoid the inlining
*/

// will return either a value set or value set row depeding on
// value set table name
// we may not need the generic here, react-query probably handles that. But it couldn't hurt?
const getLatestData = <T>(tableName: string, version: number) => {
    const endpointHeader = lookupTableApi.getTableData<T>(version, tableName);

    return axios
        .get<T>(endpointHeader.url, endpointHeader)
        .then((response) => response.data);
};

// not going to type this yet, as this function will likely go away when we bring in fetch by provider
// returns a list of all lookup tables to be filtered
const getLookupTables = () =>
    axios(lookupTableApi.getTableList()).then(({ data }) => data);

const endpointHeaderUpdate = lookupTableApi.saveTableData(
    LookupTables.VALUE_SET_ROW
);

const updateValueSet = (data: ValueSetRow[]) =>
    axios
        .post(endpointHeaderUpdate.url, data, endpointHeaderUpdate)
        .then(({ data }) => data);

const activateValueSet = (tableVersion: number) => {
    const endpointHeaderActivate = lookupTableApi.activateTableData(
        tableVersion,
        LookupTables.VALUE_SET_ROW
    );

    return axios
        .put(
            endpointHeaderActivate.url,
            LookupTables.VALUE_SET_ROW,
            endpointHeaderActivate
        )
        .then(({ data }) => data)
        .catch((e) => console.error("***", e));
};

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

export const useValueSetsTable = <T extends ValueSet | ValueSetRow>(
    dataTableName: string,
    suppliedVersion?: number
): {
    valueSetArray: Array<T>;
    error: any;
} => {
    let versionData: Partial<TableAttributes> | null;
    let error;
    let valueSetArray;

    // get all lookup tables
    const { error: tableError, data: tableData } = useQuery<LookupTable[]>(
        ["lookupTables"],
        () => getLookupTables(),
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

    // not entirely accurate typing. What is sent back by the api is actually ApiValueSet[] rather than ValueSet[]
    // does not seem entirely worth it to add the complexity needed to account for that on the frontend, better
    // to make the API conform better to the frontend's expectations. TODO: look at this when refactoring the API
    const { error: valueSetError, data: valueSetData } = useQuery<Array<T>>(
        ["lookupTable", versionData?.version, dataTableName],
        () => getLatestData<Array<T>>(dataTableName, versionData!.version!!),
        { enabled: !!versionData?.version }
    );

    // the logic here is that the value set request should not go out if there is an error
    // in the version call, so an error in the version call should preempt an error in the
    // data call
    error = error || tableError || valueSetError || null;
    valueSetArray = valueSetData
        ? valueSetData.map((el) => ({ ...el, ...versionData }))
        : [];

    return { error, valueSetArray };
};

/* 

  Mutation Hooks

  */
export const useValueSetUpdate = () => {
    // generic signature is defined here https://github.com/TanStack/query/blob/4690b585722d2b71d9b87a81cb139062d3e05c9c/packages/react-query/src/useMutation.ts#L66
    // <type of data returned, type of error returned, type of variables passed to mutate fn, type of context (?)>
    const mutation = useMutation<LookupTable, Error, ValueSetRow[]>(
        updateValueSet
    );
    return {
        saveData: mutation.mutateAsync,
        isSaving: mutation.isLoading,
        saveError: mutation.error,
    };
};

export const useValueSetActivation = () => {
    const mutation = useMutation<LookupTable, Error, number>(activateValueSet);
    return {
        activateTable: mutation.mutateAsync,
        isActivating: mutation.isLoading,
        activationError: mutation.error,
    };
};
