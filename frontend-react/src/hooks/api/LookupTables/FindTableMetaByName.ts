/*

  Helper function to find the table we want within the response for ALL tables
  Hopefully this will go away with the API refactor

*/
export const findTableMetaByName = (
    tableName: string,
    tables: LookupTable[] = []
): LookupTable => {
    if (!tables.length) {
        return {} as LookupTable;
    }
    const filteredBody: LookupTable[] = tables.filter(
        (tv: LookupTable) => tv.tableName === tableName && tv.isActive
    );

    if (!filteredBody.length) {
        console.log("Unable to find metadata for lookup table: ", tableName);
        return {} as LookupTable;
    }
    return filteredBody.sort(
        (a: LookupTable, b: LookupTable) =>
            b["tableVersion"] - a["tableVersion"]
    )[0];
};
