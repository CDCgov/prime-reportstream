import { screen, render } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import ValueSetsDetail from "./ValueSetsDetail";

const fakeRows = [
    {
        display: "hi",
    },
    {
        display: "test",
    },
];

jest.mock("../../../hooks/UseLookupTable", () => ({
    useValueSetsRowTable: () => fakeRows, // for some reason, can't get this working with a proper mock
}));

jest.mock("react-router-dom", () => ({
    useParams: () => ({ valueSetName: "a-path" }),
}));

describe("ValueSetsDetail tests", () => {
    test("Renders with no errors", () => {
        render(<ValueSetsDetail />);
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Core Values");
        const datasetActionButton = screen.getByText("Add item");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(5);
        expect(title).toBeInTheDocument();
        expect(datasetActionButton).toBeInTheDocument();
        expect(rows.length).toBe(3); // +1 for header
    });

    test("Rows are editable", () => {
        render(<ValueSetsDetail />);
        const editButtons = screen.getAllByText("Edit");
        const rows = screen.getAllByRole("row");

        // assert they are present on all rows but header
        expect(editButtons.length).toEqual(rows.length - 1);

        // activate editing mode for first row
        userEvent.click(editButtons[0]);

        // assert input element is rendered in edit mode
        const input = screen.getAllByRole("textbox");
        expect(input.length).toEqual(4);
    });
});
