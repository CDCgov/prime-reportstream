import { screen } from "@testing-library/react";

import Spinner from "./Spinner";
import { renderApp } from "../utils/CustomRenderUtils";

describe("Spinner", () => {
    test("default", () => {
        renderApp(<Spinner />);

        expect(screen.getByTestId("rs-spinner")).toBeVisible();
        expect(screen.queryByTestId("spinner-message")).not.toBeInTheDocument();
    });

    test("with message", () => {
        renderApp(<Spinner message="Loading..." />);

        expect(screen.getByTestId("rs-spinner")).toBeVisible();
        expect(screen.getByText(/Loading.../)).toBeInTheDocument();
    });
});
