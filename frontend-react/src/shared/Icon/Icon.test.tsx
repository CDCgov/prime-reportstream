import { screen } from "@testing-library/react";

import Icon from "./Icon";

describe("Icon", () => {
    test("renders with aria-label", () => {
        render(<Icon name="CheckCircle" />);
        expect(screen.getByLabelText("CheckCircle")).toBeInTheDocument();
    });
});
