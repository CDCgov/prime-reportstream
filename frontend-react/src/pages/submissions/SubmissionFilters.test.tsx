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

    test("pickers render", async () => {
        /* Trussworks USWDS library uses the placeholder text in two different
         *  HTML elements, so we have to use getAllBy...() rather than getBy...()
         *  and assert that they are non-null.
         * */
        const minPickerElements = await screen.getAllByPlaceholderText(
            /start date/i
        );
        const maxPickerElements = await screen.getAllByPlaceholderText(
            /end date/i
        );

        expect(minPickerElements).not.toBeNull();
        expect(maxPickerElements).not.toBeNull();
    });
});
