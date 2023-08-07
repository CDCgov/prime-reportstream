import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { MarkdownLayout } from "./MarkdownLayout";

describe("MarkdownLayout", () => {
    test("no sidenav", () => {
        renderApp(<MarkdownLayout>Test</MarkdownLayout>);
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
    test("sidenav", () => {
        const sidenav = <>Test sidenav</>;
        renderApp(<MarkdownLayout sidenav={sidenav}>Test</MarkdownLayout>);
        expect(screen.getByRole("navigation")).toHaveTextContent(
            "Test sidenav",
        );
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
});
