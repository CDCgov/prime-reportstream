import { render, screen } from "@testing-library/react";
import { RouterProvider, createMemoryRouter } from "react-router";

import { renderApp } from "../../utils/CustomRenderUtils";

import MarkdownLayout, { lazyRouteMarkdown } from "./MarkdownLayout";
import { LayoutSidenav } from "./LayoutComponents";

describe("MarkdownLayout", () => {
    test("no sidenav", () => {
        renderApp(
            <MarkdownLayout>
                <>Test</>
            </MarkdownLayout>
        );
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
    test("sidenav", async () => {
        renderApp(
            <MarkdownLayout>
                <>
                    <LayoutSidenav>Test</LayoutSidenav>
                    Test
                </>
            </MarkdownLayout>
        );
        await screen.findByRole("navigation");
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
