import { screen } from "@testing-library/react";

import Spinner from "./Spinner";

describe("Spinner", () => {
    test("default", () => {
        render(<Spinner />);

        expect(screen.getByTestId("rs-spinner")).toBeVisible();
        expect(screen.queryByTestId("spinner-message")).not.toBeInTheDocument();
    });

    test("with message", () => {
        render(<Spinner message="Loading..." />);

        expect(screen.getByTestId("rs-spinner")).toBeVisible();
        expect(screen.getByText(/Loading.../)).toBeInTheDocument();
    });
});
