import React, { useMemo } from "react";

import { ErrorName, ErrorUI } from "../../utils/RSNetworkError";

import { GenericMessage, GenericPage } from "./content-elements/Generic";
import { UnsupportedBrowser } from "./content-elements/UnsupportedBrowser";
import { NotFound } from "./content-elements/NotFound";

/** Handles mapping to the right page or message content */
export const errorContent = (code?: ErrorName, asPage?: boolean) => {
    switch (code) {
        case ErrorName.NOT_FOUND:
            // TODO: Needs message UI
            return <NotFound />;
        case ErrorName.UNSUPPORTED_BROWSER:
            // TODO: Needs message UI
            return <UnsupportedBrowser />;
        case ErrorName.UNKNOWN:
        default:
            return asPage ? <GenericPage /> : <GenericMessage />;
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
    ui?: ErrorUI;
}
/** Generates page content for error pages and messages */
export const ErrorComponent = ({
    code,
    ui,
}: React.PropsWithChildren<ErrorPageProps>) => {
    /** Easy for ternary checks in the errorContent memo hook */
    const asPage = useMemo(() => ui === "page", [ui]);
    const content = useMemo(() => errorContent(code, asPage), [code, asPage]);
    return asPage ? (
        <ErrorPageWrapper>{content}</ErrorPageWrapper>
    ) : (
        <ErrorMessageWrapper>{content}</ErrorMessageWrapper>
    );
};
