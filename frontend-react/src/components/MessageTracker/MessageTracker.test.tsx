import { fireEvent, screen, within } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { MessageTracker } from "./MessageTracker";

describe("MessageTracker component", () => {
    beforeEach(() => {
        renderWithRouter(<MessageTracker />);
    });

    test("should be able to edit search field", () => {
        const searchField = screen.getByTestId("textInput");

        expect(searchField).toBeInTheDocument();

        fireEvent.change(searchField, { target: { value: "123" } });

        expect(searchField).toHaveValue("123");
    });

    test("should be able to clear search field", async () => {
        const searchField = screen.getByTestId("textInput");
        expect(searchField).toBeInTheDocument();

        const clearButton = await screen.findByText("Clear");
        expect(clearButton).toBeInTheDocument();

        fireEvent.click(clearButton);

        expect(searchField).toHaveValue("");
    });

    test("renders proper search results", async () => {
        const searchField = screen.getByTestId("textInput");
        expect(searchField).toBeInTheDocument();

        const submitButton = await screen.findByText("Search");
        expect(submitButton).toBeInTheDocument();

        const clearButton = await screen.findByText("Clear");
        expect(clearButton).toBeInTheDocument();

        // set input field then click submit
        const textInput = await screen.findByTestId("textInput");
        expect(textInput).toBeInTheDocument();

        fireEvent.change(textInput, { target: { value: "123" } });
        fireEvent.click(submitButton);

        const table = screen.queryByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(4); // 2 warnings + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells[0]).toHaveTextContent("12-234567");

        const secondCells = await within(rows[2]).findAllByRole("cell");
        expect(secondCells[0]).toHaveTextContent("12-234567");

        const thirdCells = await within(rows[3]).findAllByRole("cell");
        expect(thirdCells[0]).toHaveTextContent("12-234567");
    });
});
