import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { SluggedTocEntry, TableOfContents } from "./TableOfContents";

describe("TableOfContents", () => {
    const tocItems: SluggedTocEntry[] = [
        {
            attributes: { id: "section-1" },
            depth: 2,
            value: "Section 1",
            children: [
                {
                    attributes: { id: "section-2" },
                    depth: 3,
                    value: "Section 2",
                    children: [],
                },
            ],
        },
    ];

    test("renders", () => {
        renderApp(<TableOfContents items={tocItems} />);
        expect(screen.getAllByRole("list")).toHaveLength(2);
        expect(screen.getAllByRole("listitem")).toHaveLength(2);
    });

    test("renders depth", () => {
        renderApp(<TableOfContents items={tocItems} depth={2} />);
        expect(screen.getByRole("list")).toBeInTheDocument();
        expect(screen.getByRole("listitem")).toHaveTextContent("Section 1");
    });
});
