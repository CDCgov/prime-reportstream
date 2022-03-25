import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import SubmissionFilters from "./SubmissionFilters";

describe("Rendering", () => {
    beforeEach(() => {
        renderWithRouter(<SubmissionFilters />);
    });

    test("renders without error", async () => {
        const container = await screen.findByTestId("filter-container");
        expect(container).toBeInTheDocument();
    });

    test("pickers render", () => {
        /* Trussworks USWDS library uses the placeholder text in two different
         *  HTML elements, so we have to use getAllBy...() rather than getBy...()
         *  and assert that they are non-null.
         * */
        const minPickerElements = screen.getByPlaceholderText(/start date/i);
        const maxPickerElements = screen.getByPlaceholderText(/end date/i);

        expect(minPickerElements).toBeInTheDocument();
        expect(maxPickerElements).toBeInTheDocument();
    });
});
