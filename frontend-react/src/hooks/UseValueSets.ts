import { AxiosRequestConfig } from "axios";
import { useQuery, useMutation } from "@tanstack/react-query";
import omit from "lodash.omit";

import {
    LookupTable,
    LookupTables,
    ValueSet,
    ValueSetRow,
} from "../network/api/LookupTableApi";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { EndpointConfig, HTTPMethods } from "./UseCreateFetch";

export interface TableAttributes {
    version: number;
    createdAt?: string;
    createdBy?: string;
}

// these can be stored on our resource once we build that out
// notice the react-router style colon demarcated dynamic path segments
const getTableListConfig = {
    path: "/lookuptables/list",
    method: HTTPMethods.GET,
};
const getTableDataConfig = {
    path: "/lookuptables/:tableName/:version/content",
    method: HTTPMethods.GET,
};

const updateTableConfig = {
    path: "/lookuptables/:tableName",
    method: HTTPMethods.POST,
};
const activateTableConfig = {
    path: "/lookuptables/:tableName/:version/activate",
    method: HTTPMethods.PUT,
};

interface Segments {
    [segmentKey: string]: string;
}

interface AxiosOptionsWithSegments extends AxiosRequestConfig {
    segments: Segments;
}

// takes a map of path segment keys (as defined by colons in config paths declarations, such as in react outer)
// to segment values for a particular API call
// in order to produce concrete path from a dynamic one
// this stuff likely will live on even in a world with resources, one way or another
const hydrateDynamicPathSegments = (path: string, segments?: Segments) => {
    if (!segments) {
        return path;
    }
    const pathWithSegments = Object.entries(segments).reduce(
        (pathWithSegments, [segmentKey, segmentValue]) =>
            pathWithSegments.replace(`:${segmentKey}`, segmentValue),
        path
    );
    if (pathWithSegments.indexOf("/:") > -1) {
        throw new Error(`missing dynamic path param: ${path}, ${segments}`);
    }
    return pathWithSegments;
};

// this could live on each resource, which would preclude the need to pass in the config, as that
// would also be stored on the resource
const toFetchParams = (
    baseConfig: EndpointConfig,
    requestParams: Partial<AxiosOptionsWithSegments>
) => {
    const pathWithSegments = hydrateDynamicPathSegments(
        baseConfig.path,
        requestParams.segments
    );
    return {
        method: baseConfig.method,
        path: pathWithSegments,
        options: { ...omit(requestParams, "segments") }, // this is yucky
    };
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

    // multiple calls to the hook for different types is awkward but
    // will be less awkward once resources are introduced, as those will be passed in
    // OR we could move to a world where this hook just returns the generator function, and
    // we call the generator function within our component called hooks to type the functions?
    const lookupTableFetch = useAuthorizedFetch<LookupTable[]>();
    const dataFetch = useAuthorizedFetch<T[]>();

    // get all lookup tables
    const { error: tableError, data: tableData } = useQuery<LookupTable[]>(
        ["lookupTables"],
        () => lookupTableFetch(getTableListConfig),
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
        // note that fetch function is an inlined anonymous function in order to allow for
        // useQuery to pick up changes to variables that the function is dependent on
        // if we pass in a predefined function, that exact function will be called on every render
        // and no dynamic variables will be updated
        () =>
            dataFetch(
                toFetchParams(getTableDataConfig, {
                    segments: {
                        tableName: dataTableName,
                        version: `${versionData!!.version!}`, // number to string,
                    },
                })
            ),
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
    const valueSetFetch = useAuthorizedFetch<LookupTable>();

    const updateValueSet = (data: ValueSetRow[]) => {
        return valueSetFetch(
            toFetchParams(updateTableConfig, {
                segments: { tableName: LookupTables.VALUE_SET_ROW },
                data,
            })
        );
    };

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
    const valueSetFetch = useAuthorizedFetch<LookupTable>();
    const activateValueSet = (tableVersion: number) => {
        return valueSetFetch(
            toFetchParams(activateTableConfig, {
                segments: {
                    // would be nice to hardcode this better
                    tableName: LookupTables.VALUE_SET_ROW,
                    version: `${tableVersion}`,
                },
            })
        );
    };
    const mutation = useMutation<LookupTable, Error, number>(activateValueSet);
    return {
        activateTable: mutation.mutateAsync,
        isActivating: mutation.isLoading,
        activationError: mutation.error,
    };
};
