import { useEffect, useState } from "react";
import axios from "axios";

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

export async function getLatestVersion(
    tableName: LookupTables
): Promise<TableAttributes> {
    const response = await axios(lookupTableApi.getTableList()).then(
        (response) => response.data
    );

    let filteredBody: LookupTable[] = response.filter(
        (tv: LookupTable) => tv.tableName === tableName && tv.isActive
    );

    const table: LookupTable = filteredBody.sort(
        (a: LookupTable, b: LookupTable) =>
            b["tableVersion"] - a["tableVersion"]
    )[0];

    if (!table) {
        throw new Error(`Table '${tableName}' was not found!`);
    }

    const { tableVersion, createdAt, createdBy } = table;
    if (tableVersion === undefined) {
        throw new Error(`No version of table '${tableName}' was found!`);
    }

    return {
        version: tableVersion,
        createdAt,
        createdBy,
    };
}

export async function getLatestData<T>(
    version: number,
    tableName: string
): Promise<T | T[]> {
    const endpointHeader = lookupTableApi.getTableData<T>(version, tableName);

    return await axios
        .get<T>(endpointHeader.url, endpointHeader)
        .then((response) => response.data);
}

const getDataAndVersion = async <T>(
    tableName: LookupTables,
    suppliedVersion?: number
): Promise<{
    data: any[];
    versionData: TableAttributes;
}> => {
    const versionData = suppliedVersion
        ? { version: suppliedVersion }
        : await getLatestVersion(tableName);

    const data: T | any[] = await getLatestData<T[]>(
        versionData.version,
        tableName
    );
    return { data, versionData };
};

export const getSenderAutomationData = async <T>(
    tableName: LookupTables,
    suppliedVersion?: number
): Promise<any[]> => {
    const { data, versionData } = await getDataAndVersion<T>(
        tableName,
        suppliedVersion
    );

    return data.map(
        (set: {
            // should we add version here? that is in the detail designs but seems more relevant here
            name: string;
            system: string;
            createdBy: string;
            createdAt: string;
        }) => ({
            name: set.name,
            system: set.system,
            createdBy: versionData.createdBy,
            createdAt: versionData.createdAt,
        })
    );
};

export const getSenderAutomationDataRows = async <T>(
    tableName: LookupTables,
    suppliedVersion?: number
): Promise<any[]> => {
    const { data } = await getDataAndVersion<T>(tableName, suppliedVersion);

    return data.map(
        (set: {
            name: string;
            display: string;
            code: string;
            version: string;
        }) => ({
            name: set.name,
            display: set.display,
            code: set.code,
            version: set.version,
        })
    );
};

const useLookupTable = <T>(
    tableName: LookupTables,
    dataSetName: string | null = null,
    version?: number
): { valueSetArray: T[]; error: any } => {
    const [valueSetArray, setValueSetArray] = useState<T[]>([]);
    const [error, setError] = useState();

    useEffect(() => {
        let promiseResult: Promise<any[]>;
        // since dataSetName is no longer needed for filtering here,
        // let's think of another way to switch, so that we don't have to provide that argument
        if (dataSetName !== null) {
            promiseResult = getSenderAutomationDataRows<T>(tableName, version);
        } else {
            promiseResult = getSenderAutomationData<T>(tableName, version);
        }
        promiseResult
            .then((results) => {
                setValueSetArray(results);
            })
            .catch((e) => {
                setError(e);
            });
    }, [dataSetName, tableName, version]);

    return { valueSetArray, error };
};

export const useValueSetsTable = () =>
    useLookupTable<ValueSet>(LookupTables.VALUE_SET);

export const useValueSetsRowTable = (dataSetName: string, version?: number) =>
    useLookupTable<ValueSetRow>(
        LookupTables.VALUE_SET_ROW,
        dataSetName,
        version
    );

export default useLookupTable;
