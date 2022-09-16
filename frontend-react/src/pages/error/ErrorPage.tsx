import React from "react";

import { ErrorMessage } from "../../content/error/ErrorMessages";

import { GenericError } from "./legacy-content/Generic";

/** Just  */
export const ErrorDisplayWrapper = (props: React.PropsWithChildren<{}>) => {
    return (
        <div data-testid={"error-display-wrapper"} className="grid-container">
            {props.children}
        </div>
    );
};
/** @deprecated For use with NetworkErrorBoundary until refactored out */
export const ErrorPage = ({
    type,
    config,
}: {
    type?: "message" | "page";
    config?: ErrorMessage;
}) => (
    <ErrorDisplayWrapper>
        <GenericError displayConfig={config} displayAsPage={type === "page"} />
    </ErrorDisplayWrapper>
);
