import { render, screen } from "@testing-library/react";

import { RSNetworkError } from "../utils/RSNetworkError";

import { withThrowableError } from "./RSErrorBoundary";

// Dummy components for testing
const ThrowsRSError = (): JSX.Element => {
    throw new RSNetworkError("");
};
const ThrowsGenericError = (): JSX.Element => {
    throw new Error("");
};
const ThrowsNoError = (): JSX.Element => <div>Success!</div>;

// Wrap them with error boundary
const ThrowsRSErrorWrapped = () => withThrowableError(<ThrowsRSError />);
const ThrowsGenericErrorWrapped = () =>
    withThrowableError(<ThrowsGenericError />);
const ThrowsNoErrorWrapped = () => withThrowableError(<ThrowsNoError />);

// Silences console.error and console.log of error stack
jest.spyOn(global.console, "error");
jest.spyOn(global.console, "log");

describe("RSErrorBoundary", () => {
    test("Catches RSError", () => {
        render(<ThrowsRSErrorWrapped />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });

    test("Catches legacy errors", () => {
        render(<ThrowsGenericErrorWrapped />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });

    test("Renders component when no error", () => {
        render(<ThrowsNoErrorWrapped />);
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
