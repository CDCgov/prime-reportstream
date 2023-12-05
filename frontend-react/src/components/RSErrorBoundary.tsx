import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { isRSNetworkError } from "../utils/RSNetworkError";
import { ErrorPage } from "../pages/error/ErrorPage";
import { useSessionContext } from "../contexts/Session";

import Spinner from "./Spinner";

/** Wrap components with this error boundary to catch errors thrown */
export function RSErrorBoundary(props: React.PropsWithChildren) {
    const { rsconsole, config } = useSessionContext();
    return (
        <ErrorBoundary
            fallback={<ErrorPage type="message" />}
            onError={(exception, info) => {
                if (!isRSNetworkError(exception)) {
                    console.warn(
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
/** For wrapping with RSErrorBoundary when a catch is required for a component
 * @example
 * // As proxy
 * export const MyWrappedComponent = () = withThrowableError(<MyComponent />)
 * // or in-line
 * return (
 *  <div>
 *      {withThrowableError(<MyComponent />)}
 *  </div>
 * )*/
export const withCatch = (component: JSX.Element) => (
    <RSErrorBoundary>{component}</RSErrorBoundary>
);
/** For wrapping with Suspense when a spinner is required while data loads for a component
 * @example
 * // As proxy
 * export const MyWrappedComponent = () = withSuspense(<MyComponent />)
 * // or in-line
 * return (
 *  <div>
 *      {withSuspense(<MyComponent />)}
 *  </div>
 * )*/
export const withSuspense = (component: JSX.Element) => (
    <Suspense fallback={<Spinner />}>{component}</Suspense>
);
/** For wrapping with an RSErrorBoundary and Suspense when making network calls.
 * To use these two wrappers at varying DOM levels, use {@link withCatch}
 * and {@link withSuspense}
 * @example
 * // As proxy
 * export const MyWrappedComponent = () = withNetworkCall(<MyComponent />)
 * // or in-line
 * return (
 *  <div>
 *      {withNetworkCall(<MyComponent />)}
 *  </div>
 * )
 * */
export const withCatchAndSuspense = (component: JSX.Element) => {
    return withCatch(withSuspense(component));
};

export default RSErrorBoundary;
