import { ErrorBoundary } from "react-error-boundary";

import { isRSNetworkError } from "../utils/RSNetworkError";
import { ErrorPage } from "../pages/error/ErrorPage";
import { useSessionContext } from "../contexts/Session";
import { RSConsole } from "../utils/console";

export interface RSErrorBoundaryBaseProps extends React.PropsWithChildren {
    rsconsole: RSConsole;
}

export function RSErrorBoundaryBase({
    rsconsole,
    children,
}: RSErrorBoundaryBaseProps) {
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
                rsconsole.aiError(exception, info.componentStack);
            }}
        >
            {children}
        </ErrorBoundary>
    );
}

/** Wrap components with this error boundary to catch errors thrown */
export function RSErrorBoundary(props: React.PropsWithChildren) {
    const { rsconsole } = useSessionContext();
    return <RSErrorBoundaryBase rsconsole={rsconsole} {...props} />;
}

export default RSErrorBoundary;
