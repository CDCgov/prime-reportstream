import { ErrorBoundary } from "react-error-boundary";

import { isRSNetworkError } from "../utils/RSNetworkError";
import { ErrorPage } from "../pages/error/ErrorPage";
import { useSessionContext } from "../contexts/Session";

/** Wrap components with this error boundary to catch errors thrown */
export function RSErrorBoundary(props: React.PropsWithChildren) {
    const { rsconsole, config } = useSessionContext();
    return (
        <ErrorBoundary
            fallback={<ErrorPage type="message" />}
            onError={(exception, info) => {
                if (!isRSNetworkError(exception)) {
                    rsconsole.dev(
                        "Please work to migrate all non RSError throws to use an RSError object.",
                    );
                }
                // React will always console.error all errors, regardless of boundary,
                // so just emit the telemetry.
                rsconsole._error(
                    {
                        args: [exception, info.componentStack],
                        location: window.location.href,
                    },
                    config.AI_CONSOLE_SEVERITY_LEVELS.error,
                );
            }}
            {...props}
        />
    );
}

export default RSErrorBoundary;
