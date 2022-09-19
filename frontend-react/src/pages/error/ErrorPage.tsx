import React from "react";

import { GenericError } from "./legacy-content/Generic";

/** Config to suit page-style templates */
export interface ParagraphWithTitle {
    header: string;
    paragraph: string;
}
/** Union type for declaring a title/paragraph or string-based message */
export type ErrorDisplayMessage = ParagraphWithTitle | string;

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
        <GenericError displayConfig={config} displayAsPage={type === "page"} />
    </ErrorDisplayWrapper>
);
