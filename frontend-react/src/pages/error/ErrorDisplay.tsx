import React, { useMemo } from "react";

import { ErrorName } from "../../utils/RSNetworkError";

import { GenericError, GenericErrorProps } from "./content-elements/Generic";
import { UnsupportedBrowser } from "./content-elements/UnsupportedBrowser";
import { NotFound } from "./content-elements/NotFound";

/** Handles mapping to the right page or message content */
export const errorContent = (code?: ErrorName, errorDisplayProps?: object) => {
    switch (code) {
        case ErrorName.NOT_FOUND:
            return <NotFound />;
        case ErrorName.UNSUPPORTED_BROWSER:
            return <UnsupportedBrowser />;
        case ErrorName.UNKNOWN:
        default:
            return (
                <GenericError {...(errorDisplayProps as GenericErrorProps)} />
            );
    }
};
/** Provides proper page wrapping for error pages. Includes padding for nav compensation,
 * grid-container, and a single grid row. */
const ErrorPageWrapper = (props: React.PropsWithChildren<{}>) => {
    return (
        <div
            data-testid={"error-page-wrapper"}
            className="usa-section padding-top-6"
        >
            <div className="grid-container">
                <div className="grid-row grid-gap">{props.children}</div>
            </div>
        </div>
    );
};
/** For lighter, smaller error messages. Uses grid-container to wrap and that's
 * it. Useful for error messages that don't render the whole page useless (i.e. a banner) */
const ErrorMessageWrapper = (props: React.PropsWithChildren<{}>) => {
    return (
        <div data-testid={"error-message-wrapper"} className="grid-container">
            {props.children}
        </div>
    );
};

interface ErrorPageProps {
    code?: ErrorName;
    errorInfo?: React.ErrorInfo;
    displayAsPage?: boolean;
}
/** Generates page content for error pages and messages */
export const ErrorDisplay = ({ code, displayAsPage }: ErrorPageProps) => {
    /** Easy for ternary checks in the errorContent memo hook */
    const content = useMemo(() => errorContent(code), [code]);
    return displayAsPage ? (
        <ErrorPageWrapper>{content}</ErrorPageWrapper>
    ) : (
        <ErrorMessageWrapper>{content}</ErrorMessageWrapper>
    );
};
