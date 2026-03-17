import { fireEvent, render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { Dispatch, SetStateAction, useEffect, useMemo, useState } from "react";
import { act } from "react-dom/test-utils";

import { ColumnData } from "./ColumnData";
import Table, { ColumnConfig, TableConfig } from "./Table";
import TableFilters, { TableFilterDateLabel } from "./TableFilters";
import { DatasetAction } from "./TableInfo";
import { TableRowData, TableRows } from "./TableRows";
import { filterManagerFixture } from "../../hooks/filters/filters.fixtures";
import useCursorManager, {
    CursorActionType,
} from "../../hooks/filters/UseCursorManager/UseCursorManager";
import useFilterManager, {
    FilterManager,
} from "../../hooks/filters/UseFilterManager/UseFilterManager";
import { SortSettingsActionType } from "../../hooks/filters/UseSortOrder/UseSortOrder";
import { renderApp } from "../../utils/CustomRenderUtils";
import { selectDatesFromRange } from "../../utils/TestUtils";

const mockFilterManager: FilterManager = { ...filterManagerFixture };

/* Table generation tools */

const getSetOfRows = (count: number, linkable = true) => {
    const testRows: TableRowData[] = [];
    for (let i = 0; i < count; i++) {
        // this is bad, but I couldn't figure out how to do it another way
        // without jumping through lots of typescript hoops - DWS
        const row = linkable
            ? {
                  id: i,
                  item: `Item ${i}`,
                  linkableItem: `UUID-${i}`,
                  mappedItem: i,
                  transformedValue: i,
                  sortedColumn: i,
                  locallySortedColumn: i,
                  editableColumn: i,
              }
            : {
                  id: i,
                  item: `Item ${i}`,
                  param: "param",
                  actionButtonParam: "actionButtonParam",
                  mappedItem: i,
                  transformedValue: i,
                  sortedColumn: i,
                  locallySortedColumn: i,
                  editableColumn: i,
              };
        testRows.push(row);
    }
    return testRows;
};

const makeConfigs = (sampleRow: TableRowData): ColumnConfig[] => {
    const sampleMapper = new Map<number, string>([[2, "Mapped Item"]]);
    const transformFunc = (v: any) => {
        return v === 9 ? "Transformed Value" : v;
    };
    const handleActionFunc = () => {
        return "";
    };
    const handleHasAction = () => {
        return true;
    };
    const getFeatureParams = (key: string) => {
        if (key.includes("link")) {
            return {
                link: true,
                linkBasePath: "/base/",
            };
        }
        if (key.includes("action")) {
            return {
                action: handleActionFunc,
                param: "param",
                actionButtonHandler: handleHasAction,
                actionButtonParam: "hasActionParam",
            };
        }
        return {};
    };
    return Object.keys(sampleRow).map((key) => {
        return {
            dataAttr: key,
            columnHeader: `${key[0].toUpperCase()}${key
                .slice(1)
                .toLowerCase()}`,
            feature: getFeatureParams(key),
            valueMap: key.includes("map") ? sampleMapper : undefined,
            transform: key.includes("transform") ? transformFunc : undefined,
            sortable: key.includes("sort") || key.includes("Sort"),
            localSort: key.includes("local"),
            editable: key.includes("edit"),
        } as ColumnConfig;
    });
};

const getTestConfig = (rowCount: number, linkable = true): TableConfig => {
    const testRows: TableRowData[] = getSetOfRows(rowCount, linkable);
    const colConfigs: ColumnConfig[] = makeConfigs(testRows[0]);
    return {
        rows: testRows,
        columns: colConfigs,
    };
};

const clickFilterButton = async () => {
    const filterButton = screen.getByText("Apply");
    await userEvent.click(filterButton);
};

/* Sample components for test rendering */
const mockSortUpdater = vi.spyOn(mockFilterManager, "updateSort");
const mockAction = vi.fn();
const SimpleLegend = () => <span>Simple Legend</span>;
const SimpleTable = () => (
    <Table
        config={getTestConfig(10)}
        title={"Simple Table"}
        legend={<SimpleLegend />}
        datasetAction={{
            label: "Test Action",
            method: mockAction,
        }}
        enableEditableRows
    />
);
const SimpleTableWithAction = () => (
    <Table
        config={getTestConfig(2, false)}
        title={"Simple Table With Action"}
        legend={<SimpleLegend />}
        datasetAction={{
            label: "Test Simple Table With Action",
            method: mockAction,
        }}
    />
);
const FilteredTable = () => {
    return (
        <Table config={getTestConfig(10)} filterManager={mockFilterManager} />
    );
};

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
const sampleCallback = () => void 0;

/* This component is specifically configured to help test the
 * Table component. Any  */
const TestTable = ({
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
    const [_searchTerm, setSearchTerm] = useState("");
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
    const fakeColumns: ColumnConfig[] = [
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

    const handleSearchTerm = (searchTerm: string) => {
        setSearchTerm(searchTerm);
    };

    // Control range for tests
    const fakeFilterManager: FilterManager = {
        ...filterManager,
        rangeSettings: {
            from: "2024-03-01",
            to: "2024-03-30",
        },
    };

    return (
        <>
            <StateTestRendering />
            <TableFilters
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                filterManager={fakeFilterManager}
                receivers={[
                    { value: "Receiver One", label: "Receiver One" },
                    { value: "Receiver Two", label: "Receiver Two" },
                ]}
                cursorManager={{
                    cursors,
                    hasNext,
                    hasPrev,
                    update: updateCursors,
                }}
                setSearchTerm={
                    handleSearchTerm as Dispatch<SetStateAction<string>>
                }
                setService={() => undefined}
                searchTerm={""}
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

describe("Table, basic tests", () => {
    test("Info renders", () => {
        renderApp(<SimpleTable />);
        expect(screen.getByText("Test Action")).toBeInTheDocument();
        expect(screen.getByText("Simple Legend")).toBeInTheDocument();
        expect(screen.getByText("Simple Table")).toBeInTheDocument();
    });

    test("DatasetAction fires onClick", () => {
        renderApp(<SimpleTable />);
        fireEvent.click(screen.getByText("Test Action"));
        expect(mockAction).toHaveBeenCalledTimes(1);
    });

    test("Column names render", () => {
        renderApp(<SimpleTable />);
        const idHeader = screen.getByText("Id");
        const itemHeader = screen.getByText("Item");

        expect(idHeader).toBeInTheDocument();
        expect(itemHeader).toBeInTheDocument();
    });

    test("Row values render", () => {
        renderApp(<SimpleTable />);
        expect(screen.getAllByRole("columnheader").length).toEqual(9);
        expect(screen.getAllByRole("row").length).toEqual(11); // +1 for header row
        expect(screen.getByText("Item 1")).toBeInTheDocument();
    });

    test("Edit button column renders and operates", () => {
        renderApp(<SimpleTable />);
        expect(screen.getAllByText("Edit").length).toEqual(10);
        fireEvent.click(screen.getAllByText("Edit")[0]);
        expect(screen.getAllByRole("textbox").length).toEqual(1);
    });

    test("Link columns are rendered as links", () => {
        renderApp(<SimpleTable />);
        const linkInCell = screen.getByText("UUID-1");
        expect(linkInCell).toContainHTML(
            '<a class="usa-link" href="/base/UUID-1">UUID-1</a>',
        );
    });

    test("Action columns are rendered as actions", () => {
        renderApp(<SimpleTableWithAction />);
        expect(
            screen.getByText("Test Simple Table With Action"),
        ).toBeInTheDocument();
        const actionButtonCell = screen.getAllByText("actionButtonParam")[0];
        expect(actionButtonCell).toContainHTML("actionButtonParam</button>");
    });

    test("Map columns use mapped value", () => {
        renderApp(<SimpleTable />);
        expect(screen.getByText("Mapped Item")).toBeInTheDocument();
    });

    test("Transform columns use transformed value", () => {
        renderApp(<SimpleTable />);
        expect(screen.getByText("Transformed Value")).toBeInTheDocument();
    });
});

describe("Sorting integration", () => {
    afterEach(() => void vi.clearAllMocks());
    test("(Locally) Sorting swaps on header click", () => {
        renderApp(<FilteredTable />);
        const header = screen.getByText("Locallysortedcolumn");
        // click header
        fireEvent.click(header);
        // assert calls for APPLY_LOCAL_SORT, CHANGE_COL, SWAP_ORDER
        expect(mockSortUpdater).toHaveBeenCalledWith({
            type: SortSettingsActionType.APPLY_LOCAL_SORT,
            payload: {
                locally: true,
            },
        });
        expect(mockSortUpdater).toHaveBeenCalledWith({
            type: SortSettingsActionType.CHANGE_COL,
            payload: {
                column: "locallySortedColumn",
            },
        });
        expect(mockSortUpdater).toHaveBeenCalledWith({
            type: SortSettingsActionType.SWAP_LOCAL_ORDER,
        });
        expect(mockSortUpdater).toHaveBeenCalledTimes(3);
    });

    test("(Server) Sorting swaps on header click", () => {
        renderApp(<FilteredTable />);
        const header = screen.getByText("Sortedcolumn");
        // click header
        fireEvent.click(header);
        // assert calls for APPLY_LOCAL_SORT, CHANGE_COL, SWAP_ORDER
        expect(mockSortUpdater).toHaveBeenCalledWith({
            type: SortSettingsActionType.APPLY_LOCAL_SORT,
            payload: {
                locally: false,
            },
        });
        expect(mockSortUpdater).toHaveBeenCalledWith({
            type: SortSettingsActionType.CHANGE_COL,
            payload: {
                column: "sortedColumn",
            },
        });
        expect(mockSortUpdater).toHaveBeenCalledWith({
            type: SortSettingsActionType.SWAP_ORDER,
        });
        expect(mockSortUpdater).toHaveBeenCalledTimes(3);
    });
});

/* TODO:
 *   Refactor these tests to use new functions instead of TestTable
 * */
describe(
    "Table, filter integration tests",
    () => {
        function setup() {
            renderApp(<TestTable />);
        }
        test("date range selection and clearing", async () => {
            setup();
            /* Workaround to assert changing state */
            const defaultState =
                "range: from 2000-01-01T00:00:00.000Z to 3000-01-01T00:00:00.000Z";
            expect(screen.getByText(/range:/)).toHaveTextContent(defaultState);

            await selectDatesFromRange("20", "23");
            await clickFilterButton();

            /* Assert the value of state in string has changed */
            expect(screen.getByText(/range:/)).not.toHaveTextContent(
                defaultState,
            );

            const clearButton = screen.getAllByText("Reset");
            await userEvent.click(clearButton[0]);
        });

        test("cursor sets properly according to sort order", async () => {
            setup();
            const defaultCursor = "cursor: 3000-01-01T00:00:00.000Z";
            expect(screen.getByText(/cursor:/)).toHaveTextContent(
                defaultCursor,
            );

            await selectDatesFromRange("10", "20");
            await clickFilterButton();

            expect(screen.getByText(/cursor:/)).not.toHaveTextContent(
                defaultCursor,
            );
            // Checking for inclusive date
            expect(screen.getByText(/cursor:/)).toHaveTextContent(
                /2024-03-20T23:59:00.000Z/,
            );

            // Change sort order and repeat
            await userEvent.click(screen.getByText("Column Two"));
            await selectDatesFromRange("13", "23");
            await clickFilterButton();

            // Checking for exclusive date
            expect(screen.getByText(/cursor:/)).toHaveTextContent(/00:00.000Z/);
        });
    },
    { timeout: 10000 },
);

// TODO: expand these tests. For now mainly concerned with edit / save functionality - DWS 6/13/22
describe("TableRows", () => {
    test("does not call onSave function if nothing has been updated", async () => {
        const fakeRows = getSetOfRows(2, false);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeSave = vi.fn(() => Promise.resolve());
        const fakeRowSetter = vi.fn();

        const { rerender } = render(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={undefined}
            />,
        );

        // click the edit button
        // do not edit a value
        const firstButton = screen.getAllByText("Edit")[0];
        expect(firstButton).toBeInTheDocument();
        await userEvent.click(firstButton);
        expect(fakeRowSetter).toHaveBeenCalled();
        expect(fakeRowSetter).toHaveBeenCalledWith(0);

        // `rowToEdit` state is managed in the parent component
        // as we've confirmed that the state setter has been called with 0,
        // we can rerender with that state value passed in to check the next step
        rerender(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={0}
            />,
        );

        // click save
        const saveButton = screen.getAllByText("Save")[0];
        expect(saveButton).toBeInTheDocument();
        await userEvent.click(saveButton);

        // expect onSave to have not been called
        expect(fakeSave).toHaveBeenCalledTimes(0);
    });

    test("does not call onSave function when closing edit state to edit a new row", async () => {
        const fakeRows = getSetOfRows(2, false);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeSave = vi.fn(() => Promise.resolve());
        const fakeRowSetter = vi.fn();

        const { rerender } = render(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={undefined}
            />,
        );

        // click the edit button
        // do not edit a value
        const firstButton = screen.getAllByText("Edit")[0];
        expect(firstButton).toBeInTheDocument();
        await userEvent.click(firstButton);
        expect(fakeRowSetter).toHaveBeenCalled();
        expect(fakeRowSetter).toHaveBeenCalledWith(0);

        // `rowToEdit` state is managed in the parent component
        // as we've confirmed that the state setter has been called with 0,
        // we can rerender with that state value passed in to check the next step
        rerender(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={0}
            />,
        );

        // click second edit button
        // note: at this point the first button will be a save button, as save has been
        // enabled for that row
        const secondButton = screen.getAllByText("Edit")[0];
        expect(secondButton).toBeInTheDocument();
        await userEvent.click(secondButton);

        // expect onSave to have not been called
        expect(fakeSave).toHaveBeenCalledTimes(0);
    });

    test("calls onSave function with expected props when expected", async () => {
        const fakeRows = getSetOfRows(1, false);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeSave = vi.fn(() => Promise.resolve());
        const fakeRowSetter = vi.fn();

        const { rerender } = render(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={undefined}
            />,
        );

        // click the edit button
        // do not edit a value
        const editButton = screen.getByText("Edit");
        expect(editButton).toBeInTheDocument();
        await userEvent.click(editButton);
        expect(fakeRowSetter).toHaveBeenCalled();
        expect(fakeRowSetter).toHaveBeenCalledWith(0);

        // `rowToEdit` state is managed in the parent component
        // as we've confirmed that the state setter has been called with 0,
        // we can rerender with that state value passed in to check the next step
        rerender(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={0}
            />,
        );

        // update value
        // this assumes that an input is being rendered by `ColumnData`
        const firstInput =
            screen.getByLabelText<HTMLInputElement>("editableColumn-0");
        const initialValue = firstInput.value;
        await userEvent.click(firstInput);
        await userEvent.keyboard("fakeItem");

        // click save
        const saveButton = screen.getByText("Save");
        expect(saveButton).toBeInTheDocument();
        // eslint-disable-next-line testing-library/no-unnecessary-act
        await act(async () => {
            await userEvent.click(saveButton);
        });

        // expect onSave to have been called
        expect(fakeSave).toHaveBeenCalledTimes(1);
        expect(fakeSave).toHaveBeenCalledWith({
            ...fakeRows[0],
            editableColumn: `${initialValue}fakeItem`,
        });
    });
});

// TODO: expand these tests. For now mainly concerned with edit / save functionality - DWS 6/13/22
describe("ColumnData", () => {
    test("calls passed setUpdatedRow when editable field changes", async () => {
        const fakeRows = getSetOfRows(1);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeUpdate = vi.fn((..._args: any[]) => Promise.resolve());
        renderApp(
            <tr>
                <ColumnData
                    rowIndex={0}
                    colIndex={7} // this is the editable column
                    rowData={fakeRows}
                    columnConfig={fakeColumns[7]} // this is the editable column
                    editing={true}
                    setUpdatedRow={(v, f) => void fakeUpdate(v, f)}
                />
            </tr>,
        );

        // update value
        const firstInput =
            screen.getByLabelText<HTMLInputElement>("editableColumn-0");
        const initialValue = firstInput.value;
        await userEvent.click(firstInput);
        await userEvent.keyboard("fakeItem");

        // once for each character
        expect(fakeUpdate).toHaveBeenCalledTimes(8);
        expect(fakeUpdate).toHaveBeenLastCalledWith(
            `${initialValue}fakeItem`,
            "editableColumn",
        );
    });
});

describe("Adding New Rows", () => {
    test("When custom datasetAction method not passed, adds editable row to table on datasetAction click", async () => {
        renderApp(<TestTable linkable={false} editable={true} />);

        let rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(3); // 2 data rows and 1 header row

        const addRowButton = screen.getByText("Test Action");
        await userEvent.click(addRowButton);

        rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(4);
    });

    test("All fields on new editable row are editable", async () => {
        renderApp(<TestTable linkable={false} editable={true} />);

        const addRowButton = screen.getByText("Test Action");
        await userEvent.click(addRowButton);

        const rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(4);

        const newRow = rows[3];
        const inputs = within(newRow).getAllByRole("textbox");
        expect(inputs).toHaveLength(4);
    });
});
