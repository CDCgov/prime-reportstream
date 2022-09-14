import React, { useMemo } from "react";

import { ErrorName } from "../../utils/RSNetworkError";

import { GenericError, GenericErrorProps } from "./content-elements/Generic";
import { UnsupportedBrowser } from "./content-elements/UnsupportedBrowser";
import { NoPage } from "./content-elements/NoPage";

/** Handles mapping to the right page or message content */
export const errorContent = (code?: ErrorName, errorDisplayProps?: object) => {
    switch (code) {
        case ErrorName.NO_PAGE:
            return <NoPage />;
        case ErrorName.UNSUPPORTED_BROWSER:
            return <UnsupportedBrowser />;
        case ErrorName.UNKNOWN:
        default:
            return (
                <GenericError {...(errorDisplayProps as GenericErrorProps)} />
            );
    }
};
/** For lighter, smaller error messages. Uses grid-container to wrap and that's
 * it. Useful for error messages that don't render the whole page useless (i.e. a banner) */
const ErrorMessageWrapper = (props: React.PropsWithChildren<{}>) => {
    return (
        <div data-testid={"error-display-wrapper"} className="grid-container">
            {props.children}
        </div>
    );
};

interface ErrorPageProps {
    code?: ErrorName;
    errorInfo?: React.ErrorInfo;
}
/** Generates page content for error pages and messages */
export const ErrorDisplay = ({ code }: ErrorPageProps) => {
    /** Easy for ternary checks in the errorContent memo hook */
    const content = useMemo(() => errorContent(code), [code]);
    return <ErrorMessageWrapper>{content}</ErrorMessageWrapper>;
};
