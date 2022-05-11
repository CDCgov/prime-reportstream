import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";
import { mockCursorManager } from "../../hooks/filters/mocks/MockCursorManager";
import { mockFilterManager } from "../../hooks/filters/mocks/MockFilterManager";

import TableFilters, { inclusiveDateString } from "./TableFilters";

describe("Rendering", () => {
    beforeEach(() => {
        renderWithRouter(
            <TableFilters
                filterManager={mockFilterManager}
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
        const datePickers = await screen.findAllByTestId(
            "date-picker-internal-input"
        );
        expect(datePickers).toHaveLength(2);
    });
});

describe("Helper functions", () => {
    test("inclusiveDate", () => {
        const includedDate = new Date(
            inclusiveDateString("2022-04-21")
        ).toISOString();
        expect(includedDate).toEqual("2022-04-21T23:59:59.000Z");
    });
});
