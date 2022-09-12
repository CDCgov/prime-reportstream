import { screen } from "@testing-library/react";

import testMd from "../../../content/markdown-test.md";
import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import { MarkdownDirectory } from "../MarkdownDirectory";

import IASideNavTemplate from "./IASideNavTemplate";

const testDirectories = [
    new MarkdownDirectory()
        .setTitle("Test Dir")
        .setSlug("test-dir")
        .addFile(testMd),
];

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    Navigate: () => <></>,
}));

describe("StaticPageFromDirectories", () => {
    test("Renders without error", () => {
        renderWithRouter(<IASideNavTemplate directories={testDirectories} />);
    });
    test("Renders without side-nav", () => {
        renderWithRouter(<IASideNavTemplate directories={testDirectories} />);
        const nav = screen.getByText("Test Dir");
        expect(nav).toBeInTheDocument();
        expect(nav).toHaveAttribute("href", "/test-dir");
    });
});
