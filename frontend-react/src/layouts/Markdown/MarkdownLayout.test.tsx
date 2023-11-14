import { screen } from "@testing-library/react";

import { render } from "../../utils/CustomRenderUtils";

import MarkdownLayout from "./MarkdownLayout";
import { LayoutMain, LayoutSidenav } from "./LayoutComponents";

describe("MarkdownLayout", () => {
    test("no sidenav", () => {
        render(
            <MarkdownLayout>
                <>Test</>
            </MarkdownLayout>,
        );
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
        expect(screen.getByRole("article")).toHaveTextContent("Test");
    });
    test("sidenav", async () => {
        render(
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
    test("main", async () => {
        render(
            <MarkdownLayout>
                <>
                    <LayoutMain>Test</LayoutMain>
                </>
            </MarkdownLayout>,
        );
        expect(screen.getByRole("article")).toHaveTextContent("Test");
    });
    test("sidenav + main", async () => {
        render(
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
