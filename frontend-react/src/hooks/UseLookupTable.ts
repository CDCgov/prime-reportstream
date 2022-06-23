import { useEffect, useState } from "react";
import axios from "axios";

import {
    LookupTable,
    lookupTableApi,
    LookupTables,
    ValueSet,
    ValueSetRow,
} from "../network/api/LookupTableApi";
import { showError } from "../components/AlertNotifications";

export interface TableAttributes {
    version: number;
    createdAt: string;
    createdBy: string;
}

export async function getLatestVersion(
    tableName: LookupTables
): Promise<TableAttributes | null> {
    let response;
    try {
        response = await axios(lookupTableApi.getTableList()).then(
            (response) => response.data
        );

        let filteredBody: LookupTable[] = response.filter(
            (tv: LookupTable) => tv.tableName === tableName && tv.isActive
        );
        if (filteredBody.length === 0) {
            filteredBody = response.filter(
                (tv: LookupTable) => tv.tableName === tableName
            );
        }
        const table: LookupTable = filteredBody.sort(
            (a: LookupTable, b: LookupTable) =>
                b["tableVersion"] - a["tableVersion"]
        )[0];

        if (!table) {
            showError(`ERROR! Table '${tableName}' was not found!`);
            return null;
        }

        const { tableVersion, createdAt, createdBy } = table;
        if (tableVersion === undefined) {
            showError(`ERROR! No version of table '${tableName}' was found!`);
            return null;
        }

        return {
            version: tableVersion,
            createdAt,
            createdBy,
        };
    } catch (e: any) {
        console.trace(e);
        showError(e.toString());
        return null;
    }
}

export async function getLatestData<T>(
    version: number,
    tableName: string
): Promise<T | T[]> {
    const endpointHeader = lookupTableApi.getTableData<T>(version, tableName);

    try {
        return await axios
            .get<T>(endpointHeader.url, endpointHeader)
            .then((response) => response.data);
    } catch (e: any) {
        console.trace(e);
        showError(e.toString());
        return [];
    }
}

const getDataAndVersion = async <T>(
    tableName: LookupTables
): Promise<{
    data: any[];
    versionData: TableAttributes | null;
}> => {
    const versionData = await getLatestVersion(tableName);
    if (versionData === null) return { data: [], versionData: null }; // no version found (or other error occurred)

    const data: T | any[] = await getLatestData<T[]>(
        versionData.version,
        tableName
    );
    return { data, versionData };
};

export const getSenderAutomationData = async <T>(
    tableName: LookupTables
): Promise<any[]> => {
    const { data, versionData } = await getDataAndVersion<T>(tableName);

    if (!versionData) {
        return [];
    }

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
    dataSetName: string | null = null
): Promise<any[]> => {
    const { data } = await getDataAndVersion<T>(tableName);

    return data
        .filter((f) => f.name === dataSetName)
        .map(
            (set: {
                display: string;
                code: string;
                version: string;
                system: string;
            }) => ({
                display: set.display,
                code: set.code,
                version: set.version,
                system: set.system,
            })
        );
};

const useLookupTable = <T>(
    tableName: LookupTables,
    dataSetName: string | null = null
): T[] => {
    const [valueSetArray, setValueSetArray] = useState<T[]>([]);

    useEffect(() => {
        let promiseResult: Promise<any[]>;
        if (dataSetName !== null) {
            promiseResult = getSenderAutomationDataRows<T>(
                tableName,
                dataSetName
            );
        } else {
            promiseResult = getSenderAutomationData<T>(tableName);
        }
        promiseResult.then((results) => {
            setValueSetArray(results);
        });
    }, [dataSetName, tableName]);

    return valueSetArray;
};

export const useValueSetsTable = () =>
    useLookupTable<ValueSet>(LookupTables.VALUE_SET);

export const useValueSetsRowTable = (dataSetName: string) =>
    useLookupTable<ValueSetRow>(LookupTables.VALUE_SET_ROW, dataSetName);

export default useLookupTable;
