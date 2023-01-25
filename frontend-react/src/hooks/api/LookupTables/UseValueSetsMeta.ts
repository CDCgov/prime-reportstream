import { lookupTablesEndpoints } from "../../../config/api/lookupTables";
import { LookupTables } from "../../../config/api/types";
import { useRSQuery } from "../UseRSQuery";

import { findTableMetaByName } from "./FindTableMetaByName";

/*

  useValueSetsMeta

  a useQuery based custom hook used to get metadata for a given value set

*/
export const useValueSetsMeta = (
    dataTableName: string = LookupTables.VALUE_SET
) => {
    const query = useRSQuery(lookupTablesEndpoints.lookupTables, undefined, {
        select: (data) => findTableMetaByName(dataTableName, data),
    });

    return query;
};
