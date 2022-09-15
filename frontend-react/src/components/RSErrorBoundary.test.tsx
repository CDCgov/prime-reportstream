import { render, screen } from "@testing-library/react";

import { RSNetworkError } from "../utils/RSNetworkError";

import { withThrowableError } from "./RSErrorBoundary";

// Dummy components for testing
const ThrowsRSError = ({ error = true }: { error: boolean }): JSX.Element => {
    if (error) throw new RSNetworkError("");
    return <></>;
};
const ThrowsGenericError = ({
    error = true,
}: {
    error: boolean;
}): JSX.Element => {
    if (error) throw Error("");
    return <></>;
};
const ThrowsNoError = (): JSX.Element => <div>Success!</div>;

// Wrap them with error boundary
const ThrowsRSErrorWrapped = () =>
    withThrowableError(<ThrowsRSError error={true} />);
const ThrowsGenericErrorWrapped = () =>
    withThrowableError(<ThrowsGenericError error={true} />);
const ThrowsNoErrorWrapped = () => withThrowableError(<ThrowsNoError />);

// Silences console.error and console.log of error stack
jest.spyOn(global.console, "error");
jest.spyOn(global.console, "log");

describe("RSErrorBoundary", () => {
    test("Catches RSError", () => {
        render(<ThrowsRSErrorWrapped />);
        expect(screen.getByText("An error has occurred")).toBeInTheDocument();
    });

    test("Catches legacy errors", () => {
        render(<ThrowsGenericErrorWrapped />);
        expect(screen.getByText("An error has occurred")).toBeInTheDocument();
    });

    test("Renders component when no error", () => {
        render(<ThrowsNoErrorWrapped />);
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
