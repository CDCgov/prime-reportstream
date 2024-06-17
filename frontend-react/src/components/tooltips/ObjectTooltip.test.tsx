import { screen } from "@testing-library/react";

import { ObjectTooltip } from "./ObjectTooltip";
import { renderApp } from "../../utils/CustomRenderUtils";
import { SampleTimingObj } from "../../utils/TemporarySettingsAPITypes";

const TestObjectToolTip = () => {
    return <ObjectTooltip obj={new SampleTimingObj()} />;
};

describe("ObjectTooltip", () => {
    function setup() {
        renderApp(<TestObjectToolTip />);
    }
    test("Renders stringified JSON value of obj", () => {
        setup();
        const element = screen.getByText(/00:00/);
        expect(element).toBeInTheDocument();
    });
});
