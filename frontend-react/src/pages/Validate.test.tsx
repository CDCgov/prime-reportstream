// import { screen } from "@testing-library/react";

import { render } from "@testing-library/react";

import Validate from "./Validate";

describe("Validate", () => {
    test("Renders with no errors", () => {
        render(<Validate />);
    });
});
