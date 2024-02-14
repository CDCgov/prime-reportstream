import { PropsWithChildren } from "react";

import { BasicErrorDisplay } from "./Generic";
import { ErrorDisplayMessage } from "../../content/error/ErrorMessages";

/** @deprecated Move div over to render in RSErrorBoundary when NetworkErrorBoundary is
 * removed */
export const ErrorDisplayWrapper = (props: PropsWithChildren<object>) => {
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
