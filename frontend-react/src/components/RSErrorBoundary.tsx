import React, { ErrorInfo, ReactNode, Suspense } from "react";

import {
    ErrorName,
    isRSNetworkError,
    RSNetworkError,
} from "../utils/RSNetworkError";
import { ErrorDisplay } from "../pages/error/ErrorDisplay";

import Spinner from "./Spinner";

interface ErrorBoundaryProps {
    children?: ReactNode;
}
interface ErrorBoundaryState {
    hasError: boolean;
    code?: ErrorName;
}
/** Used to define default state values on render */
const initState: ErrorBoundaryState = {
    hasError: false,
};
/** Wrap components with this error boundary to catch errors thrown */
export default class RSErrorBoundary extends React.Component<
    ErrorBoundaryProps,
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
        const useLegacy = !isRSNetworkError(error);
        if (useLegacy) {
            console.warn(
                "Please work to migrate all non RSError throws to use an RSError-extending error type."
            );
        }
        return {
            hasError: true,
            code: useLegacy ? ErrorName.NON_RS_ERROR : error.code,
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
            return <ErrorDisplay code={this.state.code} />;
        }
        return this.props.children;
    }
}
/** A nice way to keep individual uses of RSErrorBoundary out of jsx since it
 * does not take props but may live at many levels of the tree.
 * @example
 * // As proxy
 * export const MyWrappedComponent = () = withThrowableError(<MyComponent />)
 * // or in-line
 * return (
 *  <div>
 *      {withThrowableError(<MyComponent />)}
 *  </div>
 * )*/
export const withThrowableError = (component: JSX.Element) => (
    <RSErrorBoundary>{component}</RSErrorBoundary>
);
/** A nice way to keep individual uses of Suspense out of jsx since it
 * takes a non-dynamic prop.
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
/** Use in exports and JSX calls to wrap an element in an error boundary and suspense
 * Convenient for use when whole pages rely on network calls.
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
export const withNetworkCall = (component: JSX.Element) => {
    return withThrowableError(withSuspense(component));
};
