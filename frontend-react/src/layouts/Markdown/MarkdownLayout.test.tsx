import { screen } from "@testing-library/react";

import { LayoutMain, LayoutSidenav } from "./LayoutComponents";
import MarkdownLayout from "./MarkdownLayout";
import { renderApp } from "../../utils/CustomRenderUtils";

describe("MarkdownLayout", () => {
    test("no sidenav", () => {
        renderApp(
            <MarkdownLayout>
                <>Test</>
            </MarkdownLayout>,
        );
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
        expect(screen.getByRole("article")).toHaveTextContent("Test");
    });
    test("sidenav", async () => {
        renderApp(
            <MarkdownLayout>
                <>
                    <LayoutSidenav>Test</LayoutSidenav>
                    Test
                </>
            </MarkdownLayout>,
        );
        await screen.findByRole("navigation");
        expect(screen.getByRole("navigation")).toHaveTextContent("Test");
        expect(screen.getByRole("article")).toHaveTextContent("Test");
    });
    test("main", () => {
        renderApp(
            <MarkdownLayout>
                <>
                    <LayoutMain>Test</LayoutMain>
                </>
            </MarkdownLayout>,
        );
        expect(screen.getByRole("article")).toHaveTextContent("Test");
    });
    test("sidenav + main", async () => {
        renderApp(
            <MarkdownLayout>
                <>
                    <LayoutSidenav>Test</LayoutSidenav>
                    <LayoutMain>Test</LayoutMain>
                </>
            </MarkdownLayout>,
        );
        await screen.findByRole("navigation");
        expect(screen.getByRole("navigation")).toHaveTextContent("Test");
        expect(screen.getByRole("article")).toHaveTextContent("Test");
    });
});
