import { useEffect, useState } from "react";
import axios from "axios";

import {
    lookupTableApi,
    LookupTable,
    ValueSet,
    LookupTables,
} from "../network/api/LookupTableApi";
import { showError } from "../components/AlertNotifications";

export const generateUseLookupTable =
    <T>(tableName: LookupTables) =>
    () => {
        return useLookupTable<T>(tableName);
    };

export async function getLatestVersion(
    tableName: LookupTables
): Promise<number> {
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

        return table?.tableVersion;
    } catch (e: any) {
        console.trace(e);
        showError(
            `An error occurred while retrieving the latest version: ${e.toString()}`
        );
        return -1;
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
        showError(
            `An error occurred while retrieving the latest data: ${e.toString()}`
        );
        return [];
    }
}

export const getSenderAutomationData = async <T>(
    tableName: LookupTables
): Promise<any[]> => {
    const version: number = await getLatestVersion(tableName);
    if (version === undefined) {
        showError("DANGER! no version was found");
        return [];
    }
    const data: T | any[] = await getLatestData<T[]>(version, tableName);

    return data.map(
        (set: {
            name: string;
            system: string;
            createdBy: string;
            createdAt: string;
        }) => ({
            name: set.name,
            system: set.system,
            createdBy: set.createdBy,
            createdAt: set.createdAt,
        })
    );
};

const useLookupTable = <T>(tableName: LookupTables): ValueSet[] => {
    const [valueSetArray, setValueSetArray] = useState<ValueSet[]>([]);

    useEffect(() => {
        getSenderAutomationData<T>(tableName).then((results) => {
            setValueSetArray(results);
        });
    }, [tableName]);

    return valueSetArray;
};

export default useLookupTable;
