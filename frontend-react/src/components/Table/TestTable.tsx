import { useEffect, useMemo } from "react";

import useCursorManager, {
    CursorActionType,
} from "../../hooks/filters/UseCursorManager";
import useFilterManager from "../../hooks/filters/UseFilterManager";

import Table, { ColumnConfig, TableConfig } from "./Table";

const dummyRowOne = {
    one: "value one",
    two: "value two",
    three: "value three",
    four: "not a test",
    five: "transform this",
};

const dummyRowTwo = {
    one: "value one again",
    two: "value two again",
    four: "test",
    five: "transform this",
};

/* This component is specifically configured to help test the
 * Table component. Any  */
export const TestTable = () => {
    const filterManager = useFilterManager();
    const { cursors, hasNext, hasPrev, update } =
        useCursorManager("firstCursor");

    /* Ensure there's at least 1 more cursor in the cursorMap
     * to test the Next/Prev buttons. In a real application
     * the effect would call addNextCursor when the API response
     * state changes. */
    useEffect(() => {
        update({ type: CursorActionType.ADD_NEXT, payload: "secondCursor" });
    }, [update]);

    /* Mocking the sort behavior that would normally be performed by the
     * API call */
    const fakeRows = useMemo(() => {
        switch (filterManager.order) {
            case "ASC":
                return [dummyRowOne, dummyRowTwo];
            case "DESC":
                return [dummyRowTwo, dummyRowOne];
        }
    }, [filterManager.order]);

    const testTransform = (v: string) => {
        if (v === "transform this") {
            return "transformed";
        } else {
            return v;
        }
    };

    /* Configuration objects to pass to <Table> */
    const fakeColumns: Array<ColumnConfig> = [
        {
            dataAttr: "two",
            columnHeader: "Column Two",
            sortable: true,
            link: true,
            linkBasePath: "/test",
        },
        { dataAttr: "one", columnHeader: "Column One" },
        {
            dataAttr: "five",
            columnHeader: "Transform Column",
            transform: testTransform,
        },
        {
            dataAttr: "four",
            columnHeader: "Map Column",
            valueMap: new Map([["test", "mapped value"]]),
        },
    ];

    const config: TableConfig = {
        columns: fakeColumns,
        rows: fakeRows,
    };

    return (
        <Table
            config={config}
            filterManager={filterManager}
            cursorManager={{ cursors, hasNext, hasPrev, update }}
        />
    );
};
