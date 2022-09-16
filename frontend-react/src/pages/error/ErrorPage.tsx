import React from "react";

import { GenericError } from "./legacy-content/Generic";

/** Config to suit page-style templates */
export interface ErrorPageContentConfig {
    header: string;
    paragraph: string;
}
/** Union type for  */
export type ErrorDisplayConfig = ErrorPageContentConfig | string;

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
    config?: ErrorDisplayConfig;
}) => (
    <ErrorDisplayWrapper>
        <GenericError displayConfig={config} displayAsPage={type === "page"} />
    </ErrorDisplayWrapper>
);
