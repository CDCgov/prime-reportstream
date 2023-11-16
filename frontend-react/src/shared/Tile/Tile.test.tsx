import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";

import { Tile } from "./Tile";

describe("Feature rendering", () => {
    const baseFeature = {
        title: "base title",
        summary: "base summary",
    };

    function setup() {
        render(<Tile {...baseFeature} />);
    }

    test("renders without error", () => {
        setup();
        const heading = screen.getByTestId("heading");
        const summary = screen.getByTestId("summary");
        expect(heading).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
    });

    test("title and summary are correct", () => {
        setup();
        const heading = screen.getByTestId("heading");
        const summary = screen.getByTestId("summary");
        expect(heading.innerHTML).toEqual(baseFeature.title);
        expect(summary.innerHTML).toEqual(baseFeature.summary);
    });
});

describe("DeliveryMethodFeature rendering", () => {
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

    function setup() {
        render(<Tile {...deliveryFeature} />);
    }

    test("renders without error", () => {
        setup();
        const image = screen.getByTestId("img");
        const heading = screen.getByTestId("heading");
        expect(image).toBeInTheDocument();
        expect(heading).toBeInTheDocument();
    });
});

describe("LiveMapFeature rendering", () => {
    const liveMapFeature = {
        img: "test.png",
        imgAlt: "test alt",
        linkInternal: "/about/our-network",
        summary: "This is a summary",
    };

    function setup() {
        render(<Tile {...liveMapFeature} />);
    }

    test("renders without error", () => {
        setup();
        const image = screen.getByTestId("img");
        const summary = screen.getByTestId("summary");
        expect(image).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
    });
});
