import { useCallback } from "react";
import { ErrorBoundary, ErrorBoundaryProps } from "react-error-boundary";
import { createErrorLogger } from "./utils";
import { RSConsole } from "../../utils/rsConsole/rsConsole";

export type RSErrorUnknownBoundaryProps = ErrorBoundaryProps & {
    rsConsole?: RSConsole;
};

function RSErrorUnknownBoundary({ rsConsole, ...props }: RSErrorUnknownBoundaryProps) {
    const errorHandler = useCallback(() => createErrorLogger(rsConsole), [rsConsole]);
    return <ErrorBoundary onError={errorHandler} {...props} />;
}

export default RSErrorUnknownBoundary;
