import { render, screen } from "@testing-library/react";

import { RSNetworkError } from "../utils/RSError";

import RSErrorBoundary from "./RSErrorBoundary";

const TestComponent = () => {
    throw new RSNetworkError("");
    return <p>Test Failed</p>;
};
const WrappedTestComponent = () => {
    return (
        <RSErrorBoundary>
            <TestComponent />
        </RSErrorBoundary>
    );
};
// Silences console.error and console.log of error stack
jest.spyOn(global.console, "error");
jest.spyOn(global.console, "log");

test("Catches thrown error from component", () => {
    render(<WrappedTestComponent />);
    expect(screen.getByText("An error has occurred")).toBeInTheDocument();
});

test("Catches thrown error from component", () => {
    render(<WrappedTestComponent />);
    expect(screen.getByText("An error has occurred")).toBeInTheDocument();
});
