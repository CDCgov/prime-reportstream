import { screen } from "@testing-library/react";

import LiveMap from "./LiveMap";
import { renderApp } from "../../utils/CustomRenderUtils";

describe("LiveMap", () => {
    const fakeSection = {
        title: "Map section",
        type: "liveMap",
        summary: "Map summary",
        subTitle: "Sub title",
    };

    function setup() {
        renderApp(<LiveMap {...fakeSection} />);
    }

    test("renders props", () => {
        setup();
        const header = screen.getByTestId("heading");
        const summary = screen.getByTestId("summary");
        const subTitle = screen.getByTestId("subTitle");
        const map = screen.getByTestId("map");

        expect(header).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
        expect(subTitle).toBeInTheDocument();
        expect(map).toBeInTheDocument();
    });
});
