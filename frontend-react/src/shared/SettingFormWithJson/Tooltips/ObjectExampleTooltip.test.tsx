import { screen } from "@testing-library/react";

import { renderApp } from "../../../utils/CustomRenderUtils";

import ObjectExampleTooltip from "./ObjectExampleTooltip";

describe("ObjectTooltip", () => {
    function setup() {
        renderApp(<ObjectExampleTooltip obj={{ foo: "bar" }} />);
    }
    test("Renders stringified JSON value of obj", () => {
        setup();
        const element = screen.getByText(/00:00/);
        expect(element).toBeInTheDocument();
    });
});
