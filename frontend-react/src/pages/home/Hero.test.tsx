import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";
import PageHeader from "../../shared/PageHeader/PageHeader";

describe("PageHeader rendering", () => {
    beforeEach(() => {
        renderApp(<PageHeader />);
    });

    test("Title and Summary render on PageHeader", () => {
        expect(screen.getByRole("banner")).toBeInTheDocument();
    });
});
