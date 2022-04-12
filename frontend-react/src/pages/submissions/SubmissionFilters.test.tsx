import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";
import { FilterManager } from "../../hooks/filters/UseFilterManager";
import { CursorManager } from "../../hooks/filters/UseCursorManager";

import SubmissionFilters from "./SubmissionFilters";

const fakeFilterManager: FilterManager = {
    startRange: new Date("2022-01-01"),
    endRange: new Date("2022-12-31"),
    column: "",
    order: "DESC",
    count: 10,
    setRange: () => console.log("set"),
    setSort: () => console.log("set"),
    setCount: () => console.log("set"),
    resetRange: () => console.log("reset"),
    resetSort: () => console.log("reset"),
    resetCount: () => console.log("reset"),
    resetAll: () => console.log("clearAll"),
};

const fakeCursorManager: CursorManager = {
    values: {
        cursor: "",
        cursors: new Map(),
        currentIndex: 0,
        hasPrev: false,
        hasNext: false,
    },
    controller: {
        addNextCursor: (v) => {
            console.log(v);
        },
        goTo: (v) => {
            console.log(v);
        },
        reset: (v) => {
            console.log(v);
        },
    },
};

describe("Rendering", () => {
    beforeEach(() => {
        renderWithRouter(
            <SubmissionFilters
                filterManager={fakeFilterManager}
                cursorManager={fakeCursorManager}
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
