import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import MetaDisplay from "./MetaDisplay";

describe("MetaDisplay rendering object", () => {
    function setup() {
        const metaobj = {
            version: 2,
            createdBy: "McTest@example.com",
            createdAt: "1/1/2000 00:00",
        } as any;
        renderApp(<MetaDisplay metaObj={metaobj} />);
    }

    test("Check data as object rendered", () => {
        setup();
        expect(screen.getByText(/v2/i)).toBeInTheDocument();
        expect(screen.getByText(/McTest@example.com/i)).toBeInTheDocument();
        // 1/1/2000 was a Saturday.
        expect(screen.getByText(/Sat/i)).toBeInTheDocument();
        expect(screen.getByText(/2000/i)).toBeInTheDocument();
    });
});
