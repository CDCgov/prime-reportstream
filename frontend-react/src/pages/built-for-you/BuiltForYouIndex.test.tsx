import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { BUILT_FOR_YOU, BuiltForYouDropdown } from "./BuiltForYouIndex";

jest.mock("rehype-slug");
jest.mock("remark-gfm");
jest.mock("remark-toc");

test("Directories", () => {
    BUILT_FOR_YOU.forEach((dir) => {
        expect(dir.title).not.toEqual("");
        expect(!dir.slug.includes(" ")).toBeTruthy();
        expect(dir.files.length >= 1).toBeTruthy();
    });
});

test("Nav", () => {
    renderWithRouter(<BuiltForYouDropdown />);
    expect(screen.getByText("May 2022")).toBeInTheDocument();
    expect(screen.getByText("June 2022")).toBeInTheDocument();
});
