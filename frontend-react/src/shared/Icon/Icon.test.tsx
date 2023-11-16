import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";

import Icon from "./Icon";

describe("Icon", () => {
    test("renders with aria-label", () => {
        render(<Icon name="CheckCircle" />);
        expect(screen.getByLabelText("CheckCircle")).toBeInTheDocument();
    });
});
