import { render, screen, waitFor } from "@testing-library/react";
import { RouterProvider, createMemoryRouter } from "react-router";
import React from "react";

import { lazyRouteMarkdown } from "./LazyRouteMarkdown";

describe("lazyRouteMarkdown", () => {
    test("works with react-router", async () => {
        const Component = React.lazy(
            lazyRouteMarkdown(() => import("../content/home/index.mdx")),
        );
        const router = createMemoryRouter([
            {
                path: "/",
                element: <Component />,
            },
        ]);
        render(<RouterProvider router={router} />);
        await waitFor(() =>
            expect(
                screen.getByRole("heading", {
                    name: "Your single connection to simplify data transfer and improve public health",
                }),
            ).toHaveTextContent(
                "Your single connection to simplify data transfer and improve public health",
            ),
        );
    });
});
