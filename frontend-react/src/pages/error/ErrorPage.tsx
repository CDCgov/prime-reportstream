import React from "react";

import { ErrorDisplayMessage } from "../../content/error/ErrorMessages";

import { BasicErrorDisplay } from "./Generic";

/** @deprecated Move div over to render in RSErrorBoundary when NetworkErrorBoundary is
 * removed */
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
    config?: ErrorDisplayMessage;
}) => (
    <ErrorDisplayWrapper>
        <BasicErrorDisplay
            displayConfig={config}
            displayAsPage={type === "page"}
        />
    </ErrorDisplayWrapper>
);
