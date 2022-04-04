import { fireEvent, screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { TestTable } from "./TestTable";

describe("Table, basic tests", () => {
    beforeEach(() => renderWithRouter(<TestTable />));
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
        expect(allRows[2].firstChild).toHaveTextContent("value two again");
    });

    test("Elements with no mapped column aren't rendered", () => {
        expect(screen.queryByText("value three")).toBeNull();
    });
});

describe("Table, pagination button tests", () => {
    beforeEach(() => renderWithRouter(<TestTable />));

    test("Next button appears when hasNext is true", () => {
        const next = screen.getByText("Next");
        expect(next).toBeInTheDocument();
    });

    test("Next button disappears when hasNext is false", async () => {
        let next: HTMLElement | null = screen.getByText("Next");
        fireEvent.click(next);
        next = await screen.queryByText("Next");
        expect(next).toBeNull();
    });

    test("Previous button appears when hasPrevious is true", () => {
        const next = screen.getByText("Next");
        fireEvent.click(next);
        const prev = screen.queryByText("Previous");
        expect(prev).toBeInTheDocument();
    });

    test("Previous button disappears when hasPrevious is false", async () => {
        // First load
        const next = screen.getByText("Next");
        let prev = await screen.queryByText("Previous");
        expect(prev).toBeNull();

        // Simulate clicking next
        fireEvent.click(next);
        prev = screen.getByText("Previous");
        expect(prev).not.toBeNull();

        // Simulate clicking previous back to page 1
        fireEvent.click(prev);
        prev = await screen.queryByText("Previous");
        expect(prev).toBeNull();
    });
});

describe("Table, sort order tests", () => {
    beforeEach(() => renderWithRouter(<TestTable />));

    test("Click header to sort", () => {
        let allRows = screen.getAllByRole("row");
        expect(allRows[1].firstChild).toHaveTextContent("value two");

        const columnOneHeader = screen.getByText("Column Two");
        fireEvent.click(columnOneHeader);

        allRows = screen.getAllByRole("row");
        expect(allRows[1].firstChild).toHaveTextContent("value two again");
    });
});
