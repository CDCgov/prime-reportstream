import { useSuspenseQuery } from "@tanstack/react-query";
import { useEffect, useMemo } from "react";

import {
    LookupTable,
    LookupTables,
    lookupTablesEndpoints,
} from "../../../../config/endpoints/lookupTables";
import useSessionContext from "../../../../contexts/Session/useSessionContext";

const { getTableList } = lookupTablesEndpoints;

/** Helper function to find the table we want within the response for ALL tables
 * Hopefully this will go away with the API refactor
 */
const findTableMetaByName = (
    tableName: string,
    tables: LookupTable[] = [],
): LookupTable | undefined => {
    if (!tables.length) {
        return undefined;
    }
    const filteredBody: LookupTable[] = tables.filter(
        (tv: LookupTable) => tv.tableName === tableName && tv.isActive,
    );

    if (!filteredBody.length) {
        return undefined;
    }
    return filteredBody.sort(
        (a: LookupTable, b: LookupTable) => b.tableVersion - a.tableVersion,
    )[0];
};

export type UseValueSetsMetaResult = ReturnType<typeof useValueSetsMeta>;

/**
 * useValueSetsMeta
 * a useQuery based custom hook used to get metadata for a given value set
 */
const useValueSetsMeta = (dataTableName: string = LookupTables.VALUE_SET) => {
    const { rsConsole, authorizedFetch } = useSessionContext();

    // get all lookup tables in order to get metadata
    const { data: tableData, ...query } = useSuspenseQuery({
        queryKey: [getTableList.queryKey],
        queryFn: () => authorizedFetch<LookupTable[]>({}, getTableList),
    });

    const tableMeta = useMemo(
        () => findTableMetaByName(dataTableName, tableData),
        [dataTableName, tableData],
    );

    useEffect(() => {
        if (!tableMeta || tableData?.length) {
            rsConsole.info(
                "Unable to find metadata for lookup table: ",
                dataTableName,
            );
        }
    }, [tableMeta, tableData, dataTableName, rsConsole]);

    return { ...query, data: tableMeta };
};

export default useValueSetsMeta;
