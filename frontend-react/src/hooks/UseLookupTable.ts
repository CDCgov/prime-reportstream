import { useEffect, useState } from "react";
import axios from "axios";

import {
    lookupTableApi,
    LookupTableList,
    ValueSet,
} from "../network/api/LookupTableApi";

export const generateUseLookupTable =
    <T extends unknown>(tableName: string) =>
    () => {
        return useLookupTable<T>(tableName);
    };

const useLookupTable = <T extends unknown>(tableName: string): ValueSet[] => {
    const [valueSetArray, setValueSetArray] = useState<ValueSet[]>([]);

    let GetLatestVersion = async function GetLatestVersion(): Promise<number> {
        let response;
        try {
            response = await axios(lookupTableApi.getTableList()).then(
                (response) => response.data
            );

            let filteredBody: LookupTableList[] = response.filter(
                (tv: LookupTableList) =>
                    tv.tableName === "sender_automation_value_set" &&
                    tv.isActive
            );
            if (filteredBody.length === 0) {
                filteredBody = response.filter(
                    (tv: LookupTableList) =>
                        tv.tableName === "sender_automation_value_set"
                );
            }
            const table: LookupTableList = filteredBody.sort(
                (a: LookupTableList, b: LookupTableList) =>
                    b["tableVersion"] - a["tableVersion"]
            )[0];

            return table?.tableVersion;
        } catch (error) {
            return -1;
        }
    };
    let GetLatestData = async function GetLatestData<T extends unknown>(
        version: number
    ) {
        const endpointHeader = lookupTableApi.getTableData<T>(
            version,
            tableName
        );

        try {
            return await axios
                .get<T>(endpointHeader.url, endpointHeader)
                .then((response) => response.data);
        } catch (error) {
            return [];
        }
    };

    const getSenderAutomationData = async <T extends unknown>(): Promise<
        any[]
    > => {
        const version: number = await GetLatestVersion();
        if (version === undefined) {
            console.error("DANGER! no version was found");
            return [];
        }
        const data: T | any[] = await GetLatestData<T[]>(version);

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

    useEffect(() => {
        getSenderAutomationData<T>().then((results) => {
            setValueSetArray(results);
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return valueSetArray;
};

export default useLookupTable;
