import React from "react";

import { ErrorName } from "../../utils/RSNetworkError";

import { GenericError, GenericErrorProps } from "./content-elements/Generic";

/** Handles mapping to the right page or message content */
export const errorContent = (code?: ErrorName, errorDisplayProps?: object) => {
    switch (code) {
        case ErrorName.UNKNOWN:
        default:
            return (
                <GenericError {...(errorDisplayProps as GenericErrorProps)} />
            );
    }
};
/** For lighter, smaller error messages. Uses grid-container to wrap and that's
 * it. Useful for error messages that don't render the whole page useless (i.e. a banner) */
export const ErrorMessageWrapper = (props: React.PropsWithChildren<{}>) => {
    return (
        <div data-testid={"error-display-wrapper"} className="grid-container">
            {props.children}
        </div>
    );
};

export const ErrorDisplay = () => (
    <ErrorMessageWrapper>
        <GenericError />
    </ErrorMessageWrapper>
);
