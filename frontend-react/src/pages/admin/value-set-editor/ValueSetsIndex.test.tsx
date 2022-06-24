import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../../utils/CustomRenderUtils";

import ValueSetsIndex from "./ValueSetsIndex";

describe("ValueSetsIndex tests", () => {
    test("Renders with no errors", () => {
        renderWithRouter(<ValueSetsIndex />);
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Value Sets");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(4);
        expect(title).toBeInTheDocument();
        expect(rows.length).toBe(1); // +1 for
    });
});
