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
    test("render a schema item", () => {
        const { container } = render(
            <CsvSchemaDocumentationItem item={baseItem} />
        );
        expect(container.firstChild).toMatchSnapshot();
    });
});
