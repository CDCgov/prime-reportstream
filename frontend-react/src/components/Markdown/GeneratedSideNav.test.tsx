import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import GeneratedSideNav from "./GeneratedSideNav";
import { MarkdownDirectory } from "./MarkdownDirectory";

const TEST_DIRS = [
    new MarkdownDirectory("Test Dir", "/test-dir", []),
    new MarkdownDirectory("Another Dir", "/another-dir", []),
];

test("GeneratedSideNav", () => {
    renderWithRouter(<GeneratedSideNav directories={TEST_DIRS} />);
    expect(screen.getByText("Test Dir")).toBeInTheDocument();
    expect(screen.getByText("Test Dir")).toHaveAttribute("href", "/test-dir");
    expect(screen.getByText("Another Dir")).toBeInTheDocument();
    expect(screen.getByText("Another Dir")).toHaveAttribute(
        "href",
        "/another-dir"
    );
});
