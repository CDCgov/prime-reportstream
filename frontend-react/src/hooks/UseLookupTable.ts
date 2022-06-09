import { useEffect, useState } from "react";
import axios from "axios";

import {
    lookupTableApi,
    LookupTable,
    ValueSet,
    LookupTables,
} from "../network/api/LookupTableApi";

export const generateUseLookupTable =
    <T extends unknown>(tableName: LookupTables) =>
    () => {
        return useLookupTable<T>(tableName);
    };

export async function GetLatestVersion(
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
    } catch (error) {
        return -1;
    }
}

export async function GetLatestData<T extends unknown>(
    version: number,
    tableName: string
): Promise<T | T[]> {
    const endpointHeader = lookupTableApi.getTableData<T>(version, tableName);

    try {
        return await axios
            .get<T>(endpointHeader.url, endpointHeader)
            .then((response) => response.data);
    } catch (error) {
        return [];
    }
}

export const getSenderAutomationData = async <T extends unknown>(
    tableName: LookupTables
): Promise<any[]> => {
    const version: number = await GetLatestVersion(tableName);
    if (version === undefined) {
        console.error("DANGER! no version was found");
        return [];
    }
    const data: T | any[] = await GetLatestData<T[]>(version, tableName);

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

const useLookupTable = <T extends unknown>(
    tableName: LookupTables
): ValueSet[] => {
    const [valueSetArray, setValueSetArray] = useState<ValueSet[]>([]);

    useEffect(() => {
        getSenderAutomationData<T>(tableName).then((results) => {
            setValueSetArray(results);
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return valueSetArray;
};

export default useLookupTable;
