import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import { Tile } from "./Tile";

describe("Feature rendering", () => {
    const baseSection = { type: "xyz" };
    const baseFeature = {
        title: "base title",
        summary: "base summary",
    };

    beforeEach(() => {
        renderApp(<Tile section={baseSection} item={baseFeature} />);
    });

    test("renders without error", () => {
        const heading = screen.getByTestId("heading");
        const summary = screen.getByTestId("summary");
        expect(heading).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
    });

    test("title and summary are correct", () => {
        const heading = screen.getByTestId("heading");
        const summary = screen.getByTestId("summary");
        expect(heading.innerHTML).toEqual(baseFeature.title);
        expect(summary.innerHTML).toEqual(baseFeature.summary);
    });
});

describe("DeliveryMethodFeature rendering", () => {
    const deliveryMethodSection = { type: "deliveryMethods" };
    const deliveryFeature = {
        title: "delivery title",
        img: "test.png",
        imgAlt: "test alt",
        items: [
            {
                title: "item 1",
                summary: "item 1 summary",
            },
            {
                title: "item 2",
                summary: "item 2 summary",
            },
        ],
    };

    beforeEach(() => {
        renderApp(
            <Tile section={deliveryMethodSection} item={deliveryFeature} />,
        );
    });

    test("renders without error", () => {
        const image = screen.getByTestId("img");
        const heading = screen.getByTestId("heading");
        expect(image).toBeInTheDocument();
        expect(heading).toBeInTheDocument();
    });
});

describe("LiveMapFeature rendering", () => {
    const liveMapSection = { type: "liveMap" };
    const liveMapFeature = {
        img: "test.png",
        imgAlt: "test alt",
        linkInternal: "/how-it-works/where-were-live",
        summary: "This is a summary",
    };

    beforeEach(() => {
        renderApp(<Tile section={liveMapSection} item={liveMapFeature} />);
    });

    test("renders without error", () => {
        const image = screen.getByTestId("img");
        const summary = screen.getByTestId("summary");
        expect(image).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
    });
});
