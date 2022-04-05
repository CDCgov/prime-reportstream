import { useEffect, useMemo } from "react";

import useCursorManager from "../../hooks/UseCursorManager";
import useFilterManager from "../../hooks/UseFilterManager";

import Table, { ColumnConfig, TableConfig } from "./Table";

/* This component is specifically configured to help test the
 * Table component. Any  */
export const TestTable = () => {
    const { filters, update } = useFilterManager({ sortOrder: "ASC" });
    const paginator = useCursorManager("firstCursor");

    /* Ensure there's at least 1 more cursor in the cursorMap
     * to test the Next/Prev buttons. In a real application
     * the effect would call addNextCursor when the API response
     * state changes. */
    useEffect(() => {
        paginator.controller.addNextCursor("secondCursor");
    }, [paginator.controller]);

    /* Mocking the sort behavior that would normally be performed by the
     * API call */
    const fakeRows = useMemo(() => {
        switch (filters.sortOrder) {
            case "ASC":
                return [
                    {
                        one: "value one",
                        two: "value two",
                        three: "value three",
                    },
                    { one: "value one again", two: "value two again" },
                ];
            case "DESC":
                return [
                    { one: "value one again", two: "value two again" },
                    {
                        one: "value one",
                        two: "value two",
                        three: "value three",
                    },
                ];
        }
    }, [filters.sortOrder]);

    /* Configuration objects to pass to <Table> */
    const fakeColumns: Array<ColumnConfig> = [
        { dataAttr: "two", columnHeader: "Column Two", sortable: true },
        { dataAttr: "one", columnHeader: "Column One" },
    ];

    const config: TableConfig = {
        columns: fakeColumns,
        rows: fakeRows,
    };

    return (
        <Table
            config={config}
            filterManager={update}
            pageController={paginator}
        />
    );
};
