import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { act } from "react-dom/test-utils";

import {
    renderWithRouter,
    renderWithCustomWrapper,
} from "../../utils/CustomRenderUtils";
import { mockFilterManager } from "../../hooks/filters/mocks/MockFilterManager";
import { SortSettingsActionType } from "../../hooks/filters/UseSortOrder";

import { TestTable } from "./TestTable";
import Table, { ColumnConfig, TableConfig } from "./Table";
import { TableRowData, TableRows } from "./TableRows";
import { ColumnData } from "./ColumnData";
/* Table generation tools */

const getSetOfRows = (count: number, linkable: boolean = true) => {
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
    return Object.keys(sampleRow).map((key) => {
        return {
            dataAttr: key,
            columnHeader: `${key[0].toUpperCase()}${key
                .slice(1)
                .toLowerCase()}`,
            feature: key.includes("link")
                ? {
                      link: true,
                      linkBasePath: "/base/",
                  }
                : {
                      // TODO: Add actionable
                  },
            valueMap: key.includes("map") ? sampleMapper : undefined,
            transform: key.includes("transform") ? transformFunc : undefined,
            sortable: key.includes("sort") || key.includes("Sort"),
            localSort: key.includes("local"),
            editable: key.includes("edit"),
        } as ColumnConfig;
    });
};

const getTestConfig = (rowCount: number): TableConfig => {
    const testRows: TableRowData[] = getSetOfRows(rowCount);
    const colConfigs: ColumnConfig[] = makeConfigs(testRows[0]);
    return {
        rows: testRows,
        columns: colConfigs,
    };
};

/* Reusable complex actions to keep tests clean */

const selectDatesFromRange = (dayOne: string, dayTwo: string) => {
    /* Borrowed some of this from Trussworks' own tests: their
     * components are tricky to test. */
    const datePickerButtons = screen.getAllByTestId("date-picker-button");
    const startDatePickerButton = datePickerButtons[0];
    const endDatePickerButton = datePickerButtons[1];

    /* Select Start Date */
    userEvent.click(startDatePickerButton);
    const newStartDateButton = screen.getByText(`${dayOne}`);
    userEvent.click(newStartDateButton);

    /* Select End Date */
    userEvent.click(endDatePickerButton);
    const newEndDateButton = screen.getByText(`${dayTwo}`);
    userEvent.click(newEndDateButton);
};

const clickFilterButton = () => {
    const filterButton = screen.getByText("Filter");
    userEvent.click(filterButton);
};

/* Sample components for test rendering */
const mockSortUpdater = jest.spyOn(mockFilterManager, "updateSort");
const mockAction = jest.fn();
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
const FilteredTable = () => {
    return (
        <Table config={getTestConfig(10)} filterManager={mockFilterManager} />
    );
};

describe("Table, basic tests", () => {
    test("Info renders", () => {
        renderWithRouter(<SimpleTable />);
        expect(screen.getByText("Test Action")).toBeInTheDocument();
        expect(screen.getByText("Simple Legend")).toBeInTheDocument();
        expect(screen.getByText("Simple Table")).toBeInTheDocument();
    });

    test("DatasetAction fires onClick", () => {
        renderWithRouter(<SimpleTable />);
        fireEvent.click(screen.getByText("Test Action"));
        expect(mockAction).toHaveBeenCalledTimes(1);
    });

    test("Column names render", () => {
        renderWithRouter(<SimpleTable />);
        const idHeader = screen.getByText("Id");
        const itemHeader = screen.getByText("Item");

        expect(idHeader).toBeInTheDocument();
        expect(itemHeader).toBeInTheDocument();
    });

    test("Row values render", () => {
        renderWithRouter(<SimpleTable />);
        expect(screen.getAllByRole("columnheader").length).toEqual(9);
        expect(screen.getAllByRole("row").length).toEqual(11); // +1 for header row
        expect(screen.getByText("Item 1")).toBeInTheDocument();
    });

    test("Edit button column renders and operates", () => {
        renderWithRouter(<SimpleTable />);
        expect(screen.getAllByText("Edit").length).toEqual(10);
        fireEvent.click(screen.getAllByText("Edit")[0]);
        expect(screen.getAllByRole("textbox").length).toEqual(1);
    });

    test("Link columns are rendered as links", () => {
        renderWithRouter(<SimpleTable />);
        const linkInCell = screen.getByText("UUID-1");
        expect(linkInCell).toContainHTML(
            '<a class="usa-link" href="/base/UUID-1">UUID-1</a>'
        );
    });

    test("Map columns use mapped value", () => {
        renderWithRouter(<SimpleTable />);
        expect(screen.getByText("Mapped Item")).toBeInTheDocument();
    });

    test("Transform columns use transformed value", () => {
        renderWithRouter(<SimpleTable />);
        expect(screen.getByText("Transformed Value")).toBeInTheDocument();
    });
});

describe("Sorting integration", () => {
    test("(Locally) Sorting swaps on header click", () => {
        renderWithRouter(<FilteredTable />);
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
        renderWithRouter(<FilteredTable />);
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
describe("Table, filter integration tests", () => {
    beforeEach(() => renderWithRouter(<TestTable />));
    test("date range selection and clearing", () => {
        /* Workaround to assert changing state */
        const defaultState =
            "range: from 2000-01-01T00:00:00.000Z to 3000-01-01T00:00:00.000Z";
        expect(screen.getByText(/range:/)).toHaveTextContent(defaultState);

        selectDatesFromRange("20", "23");
        clickFilterButton();

        /* Assert the value of state in string has changed */
        expect(screen.getByText(/range:/)).not.toHaveTextContent(defaultState);

        const clearButton = screen.getByText("Clear");
        userEvent.click(clearButton);

        expect(screen.getByText(/range:/)).toHaveTextContent(defaultState);
    });

    test("cursor sets properly according to sort order", () => {
        const defaultCursor = "cursor: 3000-01-01T00:00:00.000Z";
        expect(screen.getByText(/cursor:/)).toHaveTextContent(defaultCursor);

        selectDatesFromRange("10", "20");
        clickFilterButton();

        expect(screen.getByText(/cursor:/)).not.toHaveTextContent(
            defaultCursor
        );
        // Checking for inclusive date
        expect(screen.getByText(/cursor:/)).toHaveTextContent(/23:59:59.000Z/);

        // Change sort order and repeat
        userEvent.click(screen.getByText("Column Two"));
        selectDatesFromRange("13", "23");
        clickFilterButton();

        // Checking for exclusive date
        expect(screen.getByText(/cursor:/)).toHaveTextContent(/00:00.000Z/);
    });
});

// TODO: expand these tests. For now mainly concerned with edit / save functionality - DWS 6/13/22
describe("TableRows", () => {
    test("does not call onSave function if nothing has been updated", () => {
        const fakeRows = getSetOfRows(2, false);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeSave = jest.fn(() => Promise.resolve());
        const fakeRowSetter = jest.fn();

        const { rerender } = renderWithCustomWrapper(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={undefined}
            />,
            "tbody"
        );

        // click the edit button
        // do not edit a value
        const firstButton = screen.getAllByText("Edit")[0];
        expect(firstButton).toBeInTheDocument();
        userEvent.click(firstButton);
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
            />
        );

        // click save
        const saveButton = screen.getAllByText("Save")[0];
        expect(saveButton).toBeInTheDocument();
        userEvent.click(saveButton);

        // expect onSave to have not been called
        expect(fakeSave).toHaveBeenCalledTimes(0);
    });

    test("does not call onSave function when closing edit state to edit a new row", () => {
        const fakeRows = getSetOfRows(2, false);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeSave = jest.fn(() => Promise.resolve());
        const fakeRowSetter = jest.fn();

        const { rerender } = renderWithCustomWrapper(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={undefined}
            />,
            "tbody"
        );

        // click the edit button
        // do not edit a value
        const firstButton = screen.getAllByText("Edit")[0];
        expect(firstButton).toBeInTheDocument();
        userEvent.click(firstButton);
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
            />
        );

        // click second edit button
        // note: at this point the first button will be a save button, as save has been
        // enabled for that row
        const secondButton = screen.getAllByText("Edit")[0];
        expect(secondButton).toBeInTheDocument();
        userEvent.click(secondButton);

        // expect onSave to have not been called
        expect(fakeSave).toHaveBeenCalledTimes(0);
    });

    test("calls onSave function with expected props when expected", async () => {
        const fakeRows = getSetOfRows(1, false);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeSave = jest.fn(() => Promise.resolve());
        const fakeRowSetter = jest.fn();

        const { rerender } = renderWithCustomWrapper(
            <TableRows
                rows={fakeRows}
                onSave={fakeSave}
                enableEditableRows={true}
                filterManager={mockFilterManager}
                columns={fakeColumns}
                setRowToEdit={fakeRowSetter}
                rowToEdit={undefined}
            />,
            "tbody"
        );

        // click the edit button
        // do not edit a value
        const editButton = screen.getByText("Edit");
        expect(editButton).toBeInTheDocument();
        userEvent.click(editButton);
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
            />
        );

        // update value
        // this assumes that an input is being rendered by `ColumnData`
        const firstInput = screen.getByLabelText(
            "editableColumn-0"
        ) as HTMLInputElement;
        const initialValue = firstInput.value;
        userEvent.click(firstInput);
        userEvent.keyboard("fakeItem");

        // click save
        const saveButton = screen.getByText("Save");
        expect(saveButton).toBeInTheDocument();
        // eslint-disable-next-line testing-library/no-unnecessary-act
        await act(async () => {
            userEvent.click(saveButton);
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
    test("calls passed setUpdatedRow when editable field changes", () => {
        const fakeRows = getSetOfRows(1);
        const fakeColumns = makeConfigs(fakeRows[0]);
        const fakeUpdate = jest.fn(() => Promise.resolve());
        renderWithCustomWrapper(
            <ColumnData
                rowIndex={0}
                colIndex={7} // this is the editable column
                rowData={fakeRows}
                columnConfig={fakeColumns[7]} // this is the editable column
                editing={true}
                setUpdatedRow={fakeUpdate}
            />,
            "tr"
        );

        // update value
        const firstInput = screen.getByLabelText(
            "editableColumn-0"
        ) as HTMLInputElement;
        const initialValue = firstInput.value;
        userEvent.click(firstInput);
        userEvent.keyboard("fakeItem");

        // once for each character
        expect(fakeUpdate).toHaveBeenCalledTimes(8);
        expect(fakeUpdate).toHaveBeenLastCalledWith(
            `${initialValue}fakeItem`,
            "editableColumn"
        );
    });
});

describe("Adding New Rows", () => {
    test("When custom datasetAction method not passed, adds editable row to table on datasetAction click", () => {
        render(<TestTable linkable={false} editable={true} />);

        let rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(3); // 2 data rows and 1 header row

        const addRowButton = screen.getByText("Test Action");
        userEvent.click(addRowButton);

        rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(4);
    });

    test("All fields on new editable row are editable", () => {
        render(<TestTable linkable={false} editable={true} />);

        const addRowButton = screen.getByText("Test Action");
        userEvent.click(addRowButton);

        const rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(4);

        const newRow = rows[3];
        const inputs = within(newRow).getAllByRole("textbox");
        expect(inputs).toHaveLength(4);
    });
});
