import { screen } from "@testing-library/react";

import Icon from "./Icon";
import { renderApp } from "../../utils/CustomRenderUtils";

describe("Icon", () => {
    test("renders with aria-label", () => {
        renderApp(<Icon name="CheckCircle" />);
        expect(screen.getByLabelText("CheckCircle")).toBeInTheDocument();
    });
});
