import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import Icon from "./Icon";

describe("Icon", () => {
    test("renders with aria-label", () => {
        renderApp(<Icon name="CheckCircle" />);
        expect(screen.getByLabelText("CheckCircle")).toBeInTheDocument();
    });
});
