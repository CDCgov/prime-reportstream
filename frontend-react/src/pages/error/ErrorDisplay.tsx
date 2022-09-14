import React from "react";

import { GenericError } from "./content-elements/Generic";
/** For lighter, smaller error messages. Uses grid-container to wrap and that's
 * it. Useful for error messages that don't render the whole page useless (i.e. a banner) */
export const ErrorMessageWrapper = (props: React.PropsWithChildren<{}>) => {
    return (
        <div data-testid={"error-display-wrapper"} className="grid-container">
            {props.children}
        </div>
    );
};
/* For use with NetworkErrorBoundary until refactored out */
export const ErrorDisplay = () => (
    <ErrorMessageWrapper>
        <GenericError />
    </ErrorMessageWrapper>
);
