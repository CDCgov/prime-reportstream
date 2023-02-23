import { screen } from "@testing-library/react";

import { renderWithBase } from "../../../utils/CustomRenderUtils";

import Feature from "./Feature";

describe("Feature rendering", () => {
    const baseSection = { type: "xyz" };
    const baseFeature = {
        title: "base title",
        summary: "base summary",
    };

    beforeEach(() => {
        renderWithBase(<Feature section={baseSection} feature={baseFeature} />);
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
        renderWithBase(
            <Feature
                section={deliveryMethodSection}
                feature={deliveryFeature}
            />
        );
    });

    test("renders without error", () => {
        const image = screen.getByTestId("image");
        const heading = screen.getByTestId("heading");
        const item1 = screen.getByTestId("item-1");
        const item2 = screen.getByTestId("item-2");
        expect(image).toBeInTheDocument();
        expect(heading).toBeInTheDocument();
        expect(item1).toBeInTheDocument();
        expect(item2).toBeInTheDocument();
    });
});

describe("LiveMapFeature rendering", () => {
    const liveMapSection = { type: "liveMap" };
    const liveMapFeature = {
        img: "test.png",
        imgAlt: "test alt",
        linkInternal: "/how-it-works/where-were-live",
    };

    beforeEach(() => {
        renderWithBase(
            <Feature section={liveMapSection} feature={liveMapFeature} />
        );
    });

    test("renders without error", () => {
        const heading = screen.getByTestId("heading");
        const summary = screen.getByTestId("summary");
        expect(heading).toBeInTheDocument();
        expect(summary).toBeInTheDocument();
    });
});
