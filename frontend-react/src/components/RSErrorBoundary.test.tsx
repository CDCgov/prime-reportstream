import { screen } from "@testing-library/react";
import { AxiosError } from "axios";

import { renderApp } from "../utils/CustomRenderUtils";
import { RSNetworkError } from "../utils/RSNetworkError";
import { conditionallySuppressConsole } from "../utils/TestUtils";

import { withCatch } from "./RSErrorBoundary";

// Dummy components for testing
const ThrowsRSError = ({ error = true }: { error: boolean }): JSX.Element => {
    if (error) throw new RSNetworkError(new AxiosError("mock"));
    return <></>;
};
const ThrowsGenericError = ({
    error = true,
}: {
    error: boolean;
}): JSX.Element => {
    if (error) throw Error("mock");
    return <></>;
};
const ThrowsNoError = (): JSX.Element => <div>Success!</div>;

// Wrap them with error boundary
const ThrowsRSErrorWrapped = () => withCatch(<ThrowsRSError error={true} />);
const ThrowsGenericErrorWrapped = () =>
    withCatch(<ThrowsGenericError error={true} />);
const ThrowsNoErrorWrapped = () => withCatch(<ThrowsNoError />);

describe("RSErrorBoundary", () => {
    test("Catches RSError", () => {
        const restore = conditionallySuppressConsole(
            "unknown-error",
            "The above error occurred in the <ThrowsRSError> component:",
        );
        renderApp(<ThrowsRSErrorWrapped />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content.",
            ),
        ).toBeInTheDocument();
        restore();
    });

    test("Catches legacy errors", () => {
        const restore = conditionallySuppressConsole(
            "Please work to migrate all non RSError throws to use an RSError object.",
            "Error: mock",
            "The above error occurred in the <ThrowsGenericError> component:",
            "unknown-error",
        );
        renderApp(<ThrowsGenericErrorWrapped />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content.",
            ),
        ).toBeInTheDocument();
        restore();
    });

    test("Renders component when no error", () => {
        renderApp(<ThrowsNoErrorWrapped />);
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
