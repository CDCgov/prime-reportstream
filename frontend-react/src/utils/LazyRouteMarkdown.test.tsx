import { render, screen } from "@testing-library/react";
import { lazy } from "react";
import { createMemoryRouter, RouterProvider } from "react-router";

import { lazyRouteMarkdown } from "./LazyRouteMarkdown";

jest.mock("../contexts/Session/index", () => {
    return {
        __esModule: true,
        useSessionContext: jest.fn().mockReturnValue({
            config: {
                META: {
                    OPENGRAPH: {
                        DEFAULT_IMAGE: {
                            src: "",
                            altText: "",
                        },
                    },
                },
            },
        }),
    };
});

describe("lazyRouteMarkdown", () => {
    test("works with react-router", async () => {
        const Component = lazy(
            // eslint-disable-next-line import/no-unresolved
            lazyRouteMarkdown(() => import("../content/markdown-example.mdx")),
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
