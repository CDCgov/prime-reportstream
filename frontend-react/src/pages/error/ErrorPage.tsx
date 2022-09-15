import React from "react";

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
export const ErrorPage = ({ type }: { type?: "message" | "page" }) => (
    <ErrorDisplayWrapper>
        <GenericError displayAsPage={type === "page"} />
    </ErrorDisplayWrapper>
);
