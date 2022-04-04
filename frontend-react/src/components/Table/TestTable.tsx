import { useCallback, useEffect, useMemo, useState } from "react";

import useCursorManager from "../../hooks/UseCursorManager";

import Table, { ColumnConfig, SortOrder, TableConfig } from "./Table";

/* This component is specifically configured to help test the
 * Table component. Any  */
export const TestTable = () => {
    /* Faux sorting state. In a real application, this would be linked
     * to the sortOrder cgi parameter that, when updated, reactively
     * updates the API call and returns a new sorted response. */
    const [sort, setSort] = useState<SortOrder>("ASC");
    const changeSort = useCallback(() => {
        switch (sort) {
            case "DESC":
                setSort("ASC");
                break;
            case "ASC":
                setSort("DESC");
                break;
        }
    }, [sort]);

    /* Mocking the sort behavior that would normally be performed by the
     * API call */
    const fakeRows = useMemo(() => {
        switch (sort) {
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
    }, [sort]);

    /* Instantiate the paginator */
    const paginator = useCursorManager("firstCursor");

    /* Ensure there's at least 1 more cursor in the cursorMap
     * to test the Next/Prev buttons. In a real application
     * the effect would call addNextCursor when the API response
     * state changes. */
    useEffect(() => {
        paginator.controller.addNextCursor("secondCursor");
    }, [paginator.controller]);

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
            sortManager={changeSort}
            pageController={paginator}
        />
    );
};
