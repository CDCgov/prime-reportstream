import { screen, waitFor } from "@testing-library/react";
import React, { Suspense } from "react";

import { lazyRouteMarkdown } from "./LazyRouteMarkdown";
import { renderApp } from "./CustomRenderUtils";

describe("lazyRouteMarkdown", () => {
    test("renders", async () => {
        const Component = React.lazy(
            lazyRouteMarkdown(() => import("../content/home/index.mdx")),
        );
        renderApp(
            <Suspense>
                <Component />
            </Suspense>,
        );
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
