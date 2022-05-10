import { screen, within } from "@testing-library/react";

import { render } from "../../../utils/CustomRenderUtils";

import {
    CsvSchemaDocumentationItem,
    CsvSchemaItem,
} from "./CsvSchemaDocumentation";

const baseItem: CsvSchemaItem = {
    name: "Sample Item",
    colHeader: "sample_item",
    required: false,
    requested: false,
    acceptedFormat: false,
    acceptedValues: false,
    acceptedExample: false,
    valueType: "",
    values: [],
    notes: [],
};

describe("CsvSchemaDocumentationItem", () => {
    test("renders a schema item", () => {
        const { container } = render(
            <CsvSchemaDocumentationItem item={baseItem} />
        );
        expect(container.firstChild).toMatchSnapshot();
    });

    test("renders a required schema item", () => {
        const item = {
            ...baseItem,
            required: true,
        };
        render(<CsvSchemaDocumentationItem item={item} />);
        expect(screen.getByText("Required")).toBeInTheDocument();
    });

    test("renders a requested schema item", () => {
        const item = {
            ...baseItem,
            requested: true,
        };
        render(<CsvSchemaDocumentationItem item={item} />);
        expect(screen.queryByText("Required")).not.toBeInTheDocument();
        const header = screen.getByTestId("header");
        expect(within(header).getByText("Optional")).toBeInTheDocument();
        expect(within(header).getByText("Requested")).toBeInTheDocument();
    });

    test("renders a schema item with notes", () => {
        const item = {
            ...baseItem,
            notes: ["foo", "bar"],
        };
        render(<CsvSchemaDocumentationItem item={item} />);
        const notes = screen.getByTestId("notes");
        expect(within(notes).getByText("foo")).toBeInTheDocument();
        expect(within(notes).getByText("bar")).toBeInTheDocument();
    });
});
