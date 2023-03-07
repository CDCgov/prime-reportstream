import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { DisplayMeta } from "./DisplayMeta";

describe("DisplayMeta rendering object", () => {
    beforeEach(() => {
        const metaobj = {
            version: 2,
            createdBy: "McTest@example.com",
            createdAt: "1/1/2000 00:00",
        };
        renderApp(<DisplayMeta metaObj={metaobj} />);
    });

    test("Check data as object rendered", () => {
        expect(screen.getByText(/v2/i)).toBeInTheDocument();
        expect(screen.getByText(/McTest@example.com/i)).toBeInTheDocument();
        // 1/1/2000 was a Saturday.
        expect(screen.getByText(/Sat/i)).toBeInTheDocument();
        expect(screen.getByText(/2000/i)).toBeInTheDocument();
    });
});
