import { render, screen, waitFor } from "@testing-library/react";
import { RouterProvider, createMemoryRouter } from "react-router";

import { renderApp } from "../../utils/CustomRenderUtils";
import * as content from "../../content/markdown-example.mdx";

import MarkdownLayout, { lazyRouteMarkdown } from "./MarkdownLayout";

describe("MarkdownLayout", () => {
    test("no sidenav", () => {
        const { sidenav: _, ...frntmatter } = content.frontmatter;
        renderApp(
            <MarkdownLayout
                default={content.default}
                frontmatter={frntmatter}
            />
        );
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
    test("sidenav", async () => {
        const frontmatter = {
            ...content.frontmatter,
            sidenav: "content/markdown-sidenav-example",
        };
        renderApp(<MarkdownLayout {...content} frontmatter={frontmatter} />);
        await waitFor(() =>
            expect(screen.getByRole("navigation")).not.toHaveTextContent("...")
        );
        expect(screen.getByRole("navigation")).toHaveTextContent("Test");
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
});

describe("lazyRouteMarkdown", () => {
    test("works with react-router", async () => {
        const router = createMemoryRouter([
            {
                path: "/",
                lazy: lazyRouteMarkdown("content/markdown-example"),
            },
        ]);
        render(<RouterProvider router={router} />);
        await screen.findByRole("main");
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
});
