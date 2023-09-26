import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import SiteLink from "./SiteLink";

describe("SiteLink", () => {
    test("renders", () => {
        renderApp(<SiteLink name="assets.onepager">Download</SiteLink>);
        expect(screen.getByRole("link")).toBeInTheDocument();
    });
});
