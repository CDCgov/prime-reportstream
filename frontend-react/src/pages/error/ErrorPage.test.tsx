/* eslint-disable no-restricted-globals */
import { render, screen } from "@testing-library/react";

import { ErrorName } from "../../utils/RSError";

import { errorContent, ErrorPage } from "./ErrorPage";

describe("testing ErrorPage", () => {
    test("Renders content without code", () => {
        render(<ErrorPage type={"page"} />);
        expect(screen.getByText("An error has occurred")).toBeInTheDocument();
        render(<ErrorPage type={"message"} />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });

    test("Renders content with code", () => {
        render(<ErrorPage code={ErrorName.NOT_FOUND} type={"page"} />);
        expect(screen.getByText("Page not found")).toBeInTheDocument();
    });
});

describe("errorContent", () => {
    const genericMessageContent = errorContent(ErrorName.UNKNOWN);
    const genericPageContent = errorContent(ErrorName.UNKNOWN, true);
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
