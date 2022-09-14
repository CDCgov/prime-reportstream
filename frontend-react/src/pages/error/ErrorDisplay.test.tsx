/* eslint-disable no-restricted-globals */
import { render, screen } from "@testing-library/react";

import { ErrorName } from "../../utils/RSNetworkError";

import { errorContent, ErrorDisplay } from "./ErrorDisplay";

describe("testing ErrorPage", () => {
    test("Renders content without code", () => {
        render(<ErrorDisplay />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });

    test("Renders content with code", () => {
        render(<ErrorDisplay code={ErrorName.NOT_FOUND} />);
        expect(screen.getByText("Page not found")).toBeInTheDocument();
    });

    test("Wraps with page wrapper", () => {
        render(<ErrorDisplay displayAsPage />);
        const element = screen.getByTestId("error-page-wrapper");
        expect(element).toBeInTheDocument();
        expect(element).toHaveClass("usa-section padding-top-6");
    });

    test("Wraps with message wrapper", () => {
        render(<ErrorDisplay />);
        const element = screen.getByTestId("error-message-wrapper");
        expect(element).toBeInTheDocument();
        expect(element).toHaveClass("grid-container");
    });
});

describe("errorContent", () => {
    const genericMessageContent = errorContent(ErrorName.UNKNOWN);
    const genericPageContent = errorContent(ErrorName.UNKNOWN, {
        displayAsPage: true,
    });
    const notFoundContent = errorContent(ErrorName.NOT_FOUND);
    const unsupportedBrowserContent = errorContent(
        ErrorName.UNSUPPORTED_BROWSER
    );
    test("GenericMessage", () => {
        render(genericMessageContent);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });
    test("GenericPage", () => {
        render(genericPageContent);
        expect(screen.getByText("An error has occurred")).toBeInTheDocument();
    });
    test("NotFoundPage", () => {
        render(notFoundContent);
        expect(screen.getByText("Page not found")).toBeInTheDocument();
    });
    test("UnsupportedBrowserPage", () => {
        render(unsupportedBrowserContent);
        expect(
            screen.getByText(
                "Sorry! ReportStream does not support your browser"
            )
        ).toBeInTheDocument();
    });
});
