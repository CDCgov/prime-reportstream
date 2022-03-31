import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import Table, { ColumnConfig, RowArray, TableProps } from "./Table";

const fakeColumns: ColumnConfig = new Map([
    ["one", "Column One"],
    ["two", "Column Two"],
]);
const fakeRows: RowArray = [
    { one: "value one", two: "value two", three: "value three" },
    { two: "value two again", one: "value one again" },
];
const fakeTableProps: TableProps = {
    config: {
        columns: fakeColumns,
        rows: fakeRows,
    },
};

describe("Table, basic tests", () => {
    beforeEach(() => renderWithRouter(<Table {...fakeTableProps} />));
    test("Column names render", () => {
        const headerOne = screen.getByText("Column One");
        const headerTwo = screen.getByText("Column Two");

        expect(headerOne).toBeInTheDocument();
        expect(headerTwo).toBeInTheDocument();
    });

    test("Row values render", () => {
        const rowOne = screen.getByText("value two");
        const rowTwo = screen.getByText("value two again");

        expect(rowOne).toBeInTheDocument();
        expect(rowTwo).toBeInTheDocument();
    });

    test("Rows render data in configured order", () => {
        const allRows = screen.getAllByRole("row");
        const rowTwoElementOne = screen.getByText("value one again");
        expect(rowTwoElementOne).toEqual(allRows[2].firstChild);
    });

    test("Elements with no mapped column aren't rendered", () => {
        expect(screen.queryByText("value three") || null).toBeNull();
    });
});

// describe("Table, pagination tests", () => {
//     beforeEach(() => renderWithRouter(<TestTable />));
//     test("Pages don't exceed pageSize", () => {
//         const pageOneRows = screen.getAllByRole('row')
//         expect(pageOneRows.length).toEqual(4) // +1 for headers row
//     })
// })
