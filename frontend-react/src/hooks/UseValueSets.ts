import axios from "axios";
import { useQuery } from "@tanstack/react-query";

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

// generate a fetch function for version and table name
// will return either a value set or value set row depeding on
// value set table name

/*
  TO DISCUSS: we could set the pattern to be something like this or
  we could reference query keys within the function https://stackoverflow.com/a/68111112/5500298
  
  Have not tested, but I feel like to get the full value of react-query's refetching behavior we may want to go with the last option.
  When react-query refetches it will just re-run the function we passed to it, this would not take into account changes in variable dependencies
  unless we bake those into function itself?

  Note that I ran into type errors when trying to use higher order functions to avoid the inlining
*/

// we may not need the generic here, react-query probably handles that. But it couldn't hurt?
const getLatestData = <T>(tableName: LookupTables, version: number) => {
    const endpointHeader = lookupTableApi.getTableData<T>(version, tableName);

    return axios
        .get<T>(endpointHeader.url, endpointHeader)
        .then((response) => response.data);
};

// not going to type this yet, as this function will likely go away when we bring in fetch by provider
const getLookupTables = () =>
    axios(lookupTableApi.getTableList()).then(({ data }) => data);

const findTableByName = (
    tables: LookupTable[],
    tableName: LookupTables
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

// hook used to get value sets and value set rows (defined by passsed dataTableName)
export const useValueSetsTable = <T extends ValueSet | ValueSetRow>(
    dataTableName: LookupTables,
    suppliedVersion?: number
): {
    valueSetArray: Array<T>;
    error: any;
} => {
    // get all lookup tables
    let versionData: Partial<TableAttributes> | null;
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
        versionData = findTableByName(tableData, dataTableName);
    }

    // unclear how to resolve the issue of defining a function to account for the absence of
    // version when the `enabled` param should take care of not running if that's not present...
    const { error: valueSetError, data: valueSetData } = useQuery<Array<T>>(
        ["lookupTable", versionData?.version, dataTableName],
        () =>
            versionData?.version
                ? getLatestData<Array<T>>(dataTableName, versionData.version)
                : Promise.reject(new Error("no version")),
        { enabled: !!versionData?.version }
    );

    if (tableError || valueSetError) {
        // my logic here is that the value set request should not go out if there is an error
        // in the version call, so an error in the version call should preempt an error in the
        // data call
        return { error: tableError || valueSetError, valueSetArray: [] };
    }

    if (!valueSetData) {
        return { error: null, valueSetArray: [] };
    }

    return {
        error: null,
        // I don't think the previous map fns were doing anything helpful
        // but this will supply timestamps
        valueSetArray: valueSetData.map((el) => ({ ...el, ...versionData })),
    };
};
