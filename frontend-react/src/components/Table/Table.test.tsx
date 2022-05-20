import { fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderWithRouter } from "../../utils/CustomRenderUtils";
import { mockFilterManager } from "../../hooks/filters/mocks/MockFilterManager";
import { SortSettingsActionType } from "../../hooks/filters/UseSortOrder";

import { TestTable } from "./TestTable";
import Table, { ColumnConfig, TableConfig, TableRow } from "./Table";

/* Table generation tools */

const getSetOfRows = (count: number) => {
    const testRows: TableRow[] = [];
    for (let i = 0; i < count; i++) {
        testRows.push({
            id: i,
            item: `Item ${i}`,
            linkableItem: `UUID-${i}`,
            mappedItem: i,
            transformedValue: i,
            sortedColumn: i,
            locallySortedColumn: i,
            editableColumn: i,
        });
    }
    return testRows;
};

const makeConfigs = (sampleRow: TableRow): ColumnConfig[] => {
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
    const testRows: TableRow[] = getSetOfRows(rowCount);
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
describe("Table, pagination button tests", () => {
    beforeEach(() => renderWithRouter(<TestTable />));

    test("Next button appears when hasNext is true", () => {
        const next = screen.getByText("Next");
        expect(next).toBeInTheDocument();
    });

    test("Next button disappears when hasNext is false", () => {
        let next: HTMLElement | null = screen.getByText("Next");
        fireEvent.click(next);
        next = screen.queryByText("Next");
        expect(next).not.toBeInTheDocument();
    });

    test("Previous button appears when hasPrevious is true", () => {
        const next = screen.getByText("Next");
        fireEvent.click(next);
        const prev = screen.queryByText("Previous");
        expect(prev).toBeInTheDocument();
    });

    test("Previous button disappears when hasPrevious is false", () => {
        // First load
        const next = screen.getByText("Next");
        let prev = screen.queryByText("Previous");
        expect(prev).not.toBeInTheDocument();

        // Simulate clicking next
        fireEvent.click(next);
        prev = screen.getByText("Previous");
        expect(prev).toBeInTheDocument();

        // Simulate clicking previous back to page 1
        fireEvent.click(prev);
        prev = screen.queryByText("Previous");
        expect(prev).not.toBeInTheDocument();
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
