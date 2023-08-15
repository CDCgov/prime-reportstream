import { render, screen } from "@testing-library/react";
import { RouterProvider, createMemoryRouter } from "react-router";

import { lazyRouteMarkdown } from "./LazyRouteMarkdown";

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
