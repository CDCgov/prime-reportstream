import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";
import { SampleTimingObj } from "../../utils/TemporarySettingsAPITypes";

import { ObjectTooltip } from "./ObjectTooltip";

const TestObjectToolTip = () => {
    return <ObjectTooltip obj={new SampleTimingObj()} />;
};

describe("ObjectTooltip", () => {
    beforeEach(() => renderApp(<TestObjectToolTip />));
    test("Renders stringified JSON value of obj", () => {
        const element = screen.getByText(/00:00/);
        expect(element).toBeInTheDocument();
    });
});
