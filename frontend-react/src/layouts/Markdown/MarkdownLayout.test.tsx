import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import MarkdownLayout from "./MarkdownLayout";
import { LayoutMain, LayoutSidenav } from "./LayoutComponents";

describe("MarkdownLayout", () => {
    test("no sidenav", () => {
        renderApp(
            <MarkdownLayout>
                <>Test</>
            </MarkdownLayout>,
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
            </MarkdownLayout>,
        );
        await screen.findByRole("navigation");
        expect(screen.getByRole("navigation")).toHaveTextContent("Test");
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
    test("main", async () => {
        renderApp(
            <MarkdownLayout>
                <>
                    <LayoutMain>Test</LayoutMain>
                </>
            </MarkdownLayout>,
        );
        expect(screen.getByRole("main")).toHaveTextContent("Test");
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
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });
});
