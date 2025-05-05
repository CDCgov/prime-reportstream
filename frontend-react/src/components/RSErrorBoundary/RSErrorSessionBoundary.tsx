import { useMemo } from "react";
import { ErrorBoundary, ErrorBoundaryProps } from "react-error-boundary";
import { createErrorLogger } from "./utils";
import useSessionContext from "../../contexts/Session/useSessionContext";

/**
 * RS Error boundary that gets rsConsole from session.
 **/
function RSErrorSessionBoundary(props: ErrorBoundaryProps) {
    const { rsConsole } = useSessionContext();
    const errorHandler = useMemo(() => createErrorLogger(rsConsole), [rsConsole]);
    return <ErrorBoundary onError={errorHandler} {...props} />;
}

export default RSErrorSessionBoundary;
