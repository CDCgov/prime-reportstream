import { screen } from "@testing-library/react";
import { AxiosError } from "axios";

import { renderWithBase } from "../utils/CustomRenderUtils";
import { RSNetworkError } from "../utils/RSNetworkError";
import { conditionallySupressConsole } from "../utils/TestUtils";

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
        const restore = conditionallySupressConsole(
            "unknown-error",
            "The above error occurred in the <ThrowsRSError> component:"
        );
        renderWithBase(<ThrowsRSErrorWrapped />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
        restore();
    });

    test("Catches legacy errors", () => {
        const restore = conditionallySupressConsole(
            "Please work to migrate all non RSError throws to use an RSError object.",
            "Error: mock",
            "The above error occurred in the <ThrowsGenericError> component:",
            "unknown-error"
        );
        renderWithBase(<ThrowsGenericErrorWrapped />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
        restore();
    });

    test("Renders component when no error", () => {
        renderWithBase(<ThrowsNoErrorWrapped />);
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
