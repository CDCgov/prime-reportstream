import { screen } from "@testing-library/react";

import { renderApp } from "../utils/CustomRenderUtils";

import Spinner from "./Spinner";

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
