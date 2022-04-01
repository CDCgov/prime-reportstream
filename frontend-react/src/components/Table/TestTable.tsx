import { useEffect } from "react";

import useCursorManager from "../../hooks/UseCursorManager";

import Table, { ColumnConfig } from "./Table";

/* This component is specifically configured to help test the
 * Table component. Any  */
export const TestTable = () => {
    /* Instantiate the paginator */
    const paginator = useCursorManager("firstCursor");

    /* Ensure there's at least 1 more cursor in the cursorMap
     * to test the Next/Prev buttons */
    useEffect(() => {
        paginator.controller.addNextCursor("secondCursor");
    }, [paginator.controller]);

    /* Fake configuration objects to pass to <Table> */
    const fakeColumns: ColumnConfig = new Map([
        ["two", "Column One"],
        ["one", "Column Two"],
    ]);
    const fakeRows = [
        { one: "value one", two: "value two", three: "value three" },
        { one: "value one again", two: "value two again" },
    ];

    return (
        <Table
            config={{
                columns: fakeColumns,
                rows: fakeRows,
            }}
            pageController={{
                values: paginator.values,
                controller: paginator.controller,
            }}
        />
    );
};
