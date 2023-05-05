import { useEffect, useMemo } from "react";

import useCursorManager, {
    CursorActionType,
} from "../../hooks/filters/UseCursorManager";
import useFilterManager from "../../hooks/filters/UseFilterManager";

import Table, { ColumnConfig, TableConfig } from "./Table";
import TableFilters, { TableFilterDateLabel } from "./TableFilters";
import { DatasetAction } from "./TableInfo";

const testDataRowOne = {
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

// Exported for test purposes
export const sampleCallback = () => {
    // eslint-disable-next-line no-console
    console.log("Callback works!");
};

/* This component is specifically configured to help test the
 * Table component. Any  */
export const TestTable = ({
    editable,
    linkable = true,
}: {
    editable?: boolean;
    linkable?: boolean;
}) => {
    const filterManager = useFilterManager();
    const {
        cursors,
        hasNext,
        hasPrev,
        update: updateCursors,
    } = useCursorManager(filterManager.rangeSettings.to);

    /* Ensure there's at least 1 more cursor in the cursorMap
     * to test the Next/Prev buttons. In a real application
     * the effect would call addNextCursor when the API response
     * state changes. */
    useEffect(() => {
        updateCursors({
            type: CursorActionType.ADD_NEXT,
            payload: "secondCursor",
        });
    }, [updateCursors]);

    /* Mocking the sort behavior that would normally be performed by the
     * API call */
    const fakeRows = useMemo(() => {
        switch (filterManager.sortSettings.order) {
            case "ASC":
                return [testDataRowOne, dummyRowTwo];
            case "DESC":
                return [dummyRowTwo, testDataRowOne];
        }
    }, [filterManager.sortSettings.order]);

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
        },
        { dataAttr: "one", columnHeader: "Column One", editable: !!editable },
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

    if (linkable) {
        fakeColumns[0].feature = {
            link: true,
            linkBasePath: "/test/",
        };
    }

    const config: TableConfig = {
        columns: fakeColumns,
        rows: fakeRows,
    };

    /* To test internal state, since Enzyme isn't supported and RTL doesn't let you
     * access it, you have to render it out and query the screen for exact text */
    const StateTestRendering = () => {
        return (
            <ul>
                <li>{`range: from ${filterManager.rangeSettings.from} to ${filterManager.rangeSettings.to}`}</li>
                <li>{`cursor: ${cursors.current}`}</li>
            </ul>
        );
    };

    const Legend = () => {
        return (
            <ul>
                <li>Test legend item 1</li>
                <li>Test legend item 2</li>
            </ul>
        );
    };

    const datasetAction: DatasetAction = {
        label: "Test Action",
        method: editable ? undefined : sampleCallback,
    };

    return (
        <>
            <StateTestRendering />
            <TableFilters
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                filterManager={filterManager}
                cursorManager={{
                    cursors,
                    hasNext,
                    hasPrev,
                    update: updateCursors,
                }}
            />
            <Table
                title={"Test Table Title"}
                legend={<Legend />}
                datasetAction={datasetAction}
                config={config}
                filterManager={filterManager}
            />
        </>
    );
};
