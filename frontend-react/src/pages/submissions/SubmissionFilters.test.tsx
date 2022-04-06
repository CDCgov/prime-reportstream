import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";
import { IFilterManager } from "../../hooks/UseFilterManager";
import { ICursorManager } from "../../hooks/UseCursorManager";

import SubmissionFilters from "./SubmissionFilters";

const fakeFilterManager: IFilterManager = {
    filters: {
        startRange: "",
        endRange: "",
        sort: {
            column: "",
            order: "ASC",
        },
        pageSize: 10,
    },
    update: {
        setStartRange: () => {
            console.log("");
        },
        setEndRange: () => {
            console.log("");
        },
        setSortSettings: () => {
            console.log("");
        },
        setPageSize: () => {
            console.log("");
        },
        clearAll: () => {
            console.log("");
        },
    },
};

const fakeCursorManager: ICursorManager = {
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
