import { fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { TestTable } from "./TestTable";

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

describe("Table, basic tests", () => {
    beforeEach(() => renderWithRouter(<TestTable />));
    test("Title renders", () => {
        expect(screen.getByText("Test Table Title")).toBeInTheDocument();
    });
    test("Legend renders", () => {
        expect(screen.getAllByText(/Test legend/)).toHaveLength(2);
    });
    test("Dataset Action button renders", () => {
        const button = screen.getByText("Test Action");
        expect(button).toBeInTheDocument();
    });
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
        expect(allRows[2].firstChild).toHaveTextContent("value two");
    });

    test("Elements with no mapped column aren't rendered", () => {
        expect(screen.queryByText("value three")).not.toBeInTheDocument();
    });

    test("Link columns are rendered as links", () => {
        const linkInCell = screen.getByText("value two");
        /* This looks ugly but it's ensuring the VALUE at linkAttr
         * (or dataAttr) is plugged into the href.
         *
         * "/test/value two" is the outcome with the test data
         * but the attributes we plug in don't have spaces to account
         * for */
        expect(linkInCell).toContainHTML(
            '<a class="usa-link" href="/test/value two">value two</a>'
        );
    });

    test("Map columns use mapped value", () => {
        const mappedElement = screen.getAllByRole("row")[1].children[3];
        expect(mappedElement).toHaveTextContent("mapped value");
    });

    test("Transform columns use transformed value", () => {
        const transformedElements = screen.getAllByText("transformed");
        expect(transformedElements).toHaveLength(2);
    });
});

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

describe("Table, sort order integration tests", () => {
    beforeEach(() => renderWithRouter(<TestTable />));

    test("Click header to sort", () => {
        // order: desc
        let allRows = screen.getAllByRole("row");
        expect(allRows[1].firstChild).toHaveTextContent("value two again");

        // swap order
        const columnOneHeader = screen.getByText("Column Two");
        fireEvent.click(columnOneHeader);

        // order: asc
        allRows = screen.getAllByRole("row");
        expect(allRows[1].firstChild).toHaveTextContent("value two");
    });
});
