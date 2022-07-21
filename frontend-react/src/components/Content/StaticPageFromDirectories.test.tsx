import { match } from "react-router-dom";
import { screen } from "@testing-library/react";

import testMd from "../../content/markdown-test.md";
import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { MarkdownDirectory } from "./MarkdownDirectory";
import StaticPagesFromDirectories from "./StaticPagesFromDirectories";

const testDirectories = [
    new MarkdownDirectory("Test Dir", "test-dir", [testMd]),
];

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useRouteMatch: () => ({ path: "/test" } as match<{ path: string }>),
}));

describe("StaticPageFromDirectories", () => {
    test("Renders without error", () => {
        renderWithRouter(
            <StaticPagesFromDirectories directories={testDirectories} />
        );
    });
    test("Renders without side-nav", () => {
        renderWithRouter(
            <StaticPagesFromDirectories directories={testDirectories} />
        );
        const nav = screen.getByText("Test Dir");
        expect(nav).toBeInTheDocument();
        expect(nav).toHaveAttribute("href", "/test-dir");
    });
});
