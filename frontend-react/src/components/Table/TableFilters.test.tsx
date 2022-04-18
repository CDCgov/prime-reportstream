import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import { mockCursorManager } from "../../hooks/filters/mocks/MockCursorManager";

import TableFilters from "./TableFilters";

const fakeFilterManager: FilterManager = {
    startRange: new Date("2022-01-01"),
    endRange: new Date("2022-12-31"),
    column: "",
    order: "DESC",
    size: 10,
    setRange: () => console.log("set"),
    setSort: () => console.log("set"),
    setSize: () => console.log("set"),
    resetRange: () => console.log("reset"),
    resetSort: () => console.log("reset"),
    resetSize: () => console.log("reset"),
    resetAll: () => console.log("clearAll"),
};

describe("Rendering", () => {
    beforeEach(() => {
        renderWithRouter(
            <TableFilters
                filterManager={fakeFilterManager}
                cursorManager={mockCursorManager}
            />
        );
    });

    test("renders without error", async () => {
        const container = await screen.findByTestId("filter-container");
        expect(container).toBeInTheDocument();
    });

    test("pickers render", async () => {
        /* Trussworks USWDS library uses the placeholder text in two different
         *  HTML elements, so we have to use getAllBy...() rather than getBy...()
         *  and assert that they are non-null.
         * */
        const minPickerElements = await screen.findAllByPlaceholderText(
            "Start Date"
        );
        const maxPickerElements = await screen.findAllByPlaceholderText(
            "End Date"
        );

        expect(minPickerElements).not.toBeUndefined();
        expect(maxPickerElements).not.toBeUndefined();
        expect(minPickerElements[0]).toBeInTheDocument();
        expect(maxPickerElements[0]).toBeInTheDocument();
    });
});
