import { fireEvent, screen } from "@testing-library/react";

import { renderWithRouter } from "../../../utils/CustomRenderUtils";

import ValueSetsIndex from "./ValueSetsIndex";

describe("ValueSetsIndex (to be updated once page is constructed)", () => {
    test("Renders with no errors", () => {
        renderWithRouter(<ValueSetsIndex />);
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Value Sets");
        const legend = screen.getByTestId("table-legend");
        const datasetActionButton = screen.getByText("Add item");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(4);
        expect(title).toBeInTheDocument();
        expect(legend).toBeInTheDocument();
        expect(datasetActionButton).toBeInTheDocument();
        expect(rows.length).toBe(3); // +1 for
    });

    // Test datasetActionButton when function is written

    test("Rows are editable", () => {
        renderWithRouter(<ValueSetsIndex />);
        const editButtons = screen.getAllByText("Edit");
        const rows = screen.getAllByRole("row");

        // assert they are present on all rows but header
        expect(editButtons.length).toEqual(rows.length - 1);

        // activate editing mode for first row
        fireEvent.click(editButtons[0]);

        // assert input element is rendered in edit mode
        const input = screen.getAllByRole("textbox");
        expect(input.length).toEqual(1); // Only 1 editable item per row right now
    });
});
