import React, { useMemo } from "react";

import { ErrorName } from "../../components/RSErrorBoundary";

import { GenericMessage, GenericPage } from "./content-elements/Generic";
import { UnsupportedBrowser } from "./content-elements/UnsupportedBrowser";
import { NotFound } from "./content-elements/NotFound";

interface ErrorPageProps {
    code?: ErrorName;
    errorInfo?: React.ErrorInfo;
    type?: "page" | "message";
}
/** Provides proper page wrapping for error pages. Includes padding for nav compensation,
 * grid-container, and a single grid row. */
const ErrorPageWrapper = (props: React.PropsWithChildren<ErrorPageProps>) => {
    return (
        <div className="usa-section padding-top-6">
            <div className="grid-container">
                <div className="grid-row grid-gap">{props.children}</div>
            </div>
        </div>
    );
};
/** For lighter, smaller error messages. Uses grid-container to wrap and that's
 * it. Useful for error messages that don't render the whole page useless (i.e. a banner) */
const ErrorMessageWrapper = (
    props: React.PropsWithChildren<ErrorPageProps>
) => {
    return <div className="grid-container">{props.children}</div>;
};
/** Generates page content for error pages and messages */
export const ErrorPage = ({
    code,
    errorInfo,
    type,
}: React.PropsWithChildren<ErrorPageProps>) => {
    /** Easy for ternary checks in the errorContent memo hook */
    const asPage = useMemo(() => type === "page", [type]);
    /** Handles mapping to the right page or message content */
    const errorContent = useMemo(() => {
        switch (code) {
            case ErrorName.NOT_FOUND:
                // TODO: Needs message UI
                return <NotFound />;
            case ErrorName.UNSUPPORTED_BROWSER:
                // TODO: Needs message UI
                return <UnsupportedBrowser />;
            default:
                return asPage ? <GenericPage /> : <GenericMessage />;
        }
    }, [asPage, code]);

    return asPage ? (
        <ErrorPageWrapper>{errorContent}</ErrorPageWrapper>
    ) : (
        <ErrorMessageWrapper>{errorContent}</ErrorMessageWrapper>
    );
};

ErrorPage.defaultProps = {
    type: "message",
};
