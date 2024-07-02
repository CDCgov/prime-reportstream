import { ErrorBoundaryProps } from "react-error-boundary";
import { RSConsole } from "../../utils/rsConsole/rsConsole";
import { isRSNetworkError } from "../../utils/RSNetworkError";

/**
 * Error handler that emits through rsConsole if available.
 */
export function createErrorLogger(
    rsConsole?: RSConsole,
): Exclude<ErrorBoundaryProps["onError"], undefined> {
    return (exception, info) => {
        if (!isRSNetworkError(exception)) {
            console.warn(
                "Please work to migrate all non RSError throws to use an RSError object.",
            );
        }
        if (rsConsole) {
            // React will always console.error all errors, regardless of boundary,
            // so just emit the telemetry.
            rsConsole._error(
                {
                    args: [exception, info.componentStack],
                    location: window.location.href,
                },
                rsConsole.severityLevels.error,
            );
        }
    };
}
