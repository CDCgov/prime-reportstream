import { render, screen } from "@testing-library/react";
import { RouterProvider, createMemoryRouter } from "react-router";
import React from "react";

import { lazyRouteMarkdown } from "./LazyRouteMarkdown";

describe("lazyRouteMarkdown", () => {
    test("works with react-router", async () => {
        const Component = React.lazy(
            lazyRouteMarkdown(() => import("content/markdown-example.mdx")),
        );
        const router = createMemoryRouter([
            {
                path: "/",
                element: <Component />,
            },
        ]);
        render(<RouterProvider router={router} />);
        await screen.findByRole("article");
        expect(screen.getByRole("article")).toHaveTextContent("Test");
    });
});
