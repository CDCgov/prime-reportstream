import { render, screen } from "@testing-library/react";
import { lazy } from "react";
import { createMemoryRouter, RouterProvider } from "react-router";
import * as md from "./__mocks__/markdown-example.mdx";

import { lazyRouteMarkdown } from "./LazyRouteMarkdown";

vi.mock("react-helmet-async", () => {
    return {
        Helmet: ({ children }: any) => {
            return children;
        },
    };
});

describe("lazyRouteMarkdown", () => {
    test("works with react-router", async () => {
        const Component = lazy(
            // eslint-disable-next-line import/no-unresolved
            lazyRouteMarkdown(() => Promise.resolve(md)),
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
