import React, { ErrorInfo, PropsWithChildren, Suspense } from "react";

import {
    ErrorName,
    isRSNetworkError,
    RSNetworkError,
} from "../utils/RSNetworkError";
import { ErrorPage } from "../pages/error/ErrorPage";

import Spinner from "./Spinner";

interface ErrorBoundaryState {
    hasError: boolean;
    errorName?: ErrorName;
}
/** Used to define default state values on render */
const initState: ErrorBoundaryState = {
    hasError: false,
};
/** Wrap components with this error boundary to catch errors thrown */
export default class RSErrorBoundary extends React.Component<
    PropsWithChildren<{}>,
    ErrorBoundaryState
> {
    constructor(props: any) {
        super(props);
        this.state = initState;
    }
    /** Handles parsing error and setting up state accordingly */
    static getDerivedStateFromError(error: RSNetworkError): ErrorBoundaryState {
        /* This WILL catch non-RS errors despite our typescript type definition in the method params.
         * This is a runtime check to help with non RS errors */
        const notRSError = !isRSNetworkError(error);
        if (notRSError) {
            console.warn(
                "Please work to migrate all non RSError throws to use an RSError object."
            );
        }
        return {
            hasError: true,
            errorName: notRSError ? ErrorName.NON_RS_ERROR : error.name,
        };
    }
    /** Any developer logging needed (i.e. log the error in console, push to
     * analytics, etc.) */
    componentDidCatch(error: RSNetworkError, errorInfo: ErrorInfo) {
        console.error(error, errorInfo);
    }
    /** Renders the right error page for the right error type, OR the wrapped
     * component if no error is thrown */
    render() {
        if (this.state.hasError) {
            return <ErrorPage type={"message"} />;
        }
        return this.props.children;
    }
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
