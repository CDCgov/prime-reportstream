import { screen } from "@testing-library/react";

import { renderApp } from "../../../utils/CustomRenderUtils";
import { MarkdownDirectory } from "../MarkdownDirectory";

import GeneratedSideNav from "./GeneratedSideNav";

const TEST_DIRS = [
    new MarkdownDirectory().setTitle("Test Dir").setSlug("test-dir"),
    new MarkdownDirectory().setTitle("Another Dir").setSlug("another-dir"),
];

test("GeneratedSideNav", () => {
    renderApp(<GeneratedSideNav directories={TEST_DIRS} />);
    expect(screen.getByText("Test Dir")).toBeInTheDocument();
    expect(screen.getByText("Test Dir")).toHaveAttribute("href", "/test-dir");
    expect(screen.getByText("Another Dir")).toBeInTheDocument();
    expect(screen.getByText("Another Dir")).toHaveAttribute(
        "href",
        "/another-dir",
    );
});
