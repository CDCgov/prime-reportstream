import React, { ErrorInfo, ReactNode, Suspense } from "react";

import { ErrorName, ErrorUI, isRSError, RSError } from "../utils/RSError";
import { ErrorPage } from "../pages/error/ErrorPage";

import Spinner from "./Spinner";

interface ErrorBoundaryProps {
    children?: ReactNode;
}
interface ErrorBoundaryState {
    hasError: boolean;
    // Undefined until an error is thrown
    code?: ErrorName;
    type?: ErrorUI;
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
    static getDerivedStateFromError(error: RSError): ErrorBoundaryState {
        /* We throw a lot of errors, so refactoring seems out of scope, but the system built relies on
         * RSError functionality to render unique error pages */
        const useLegacy = !isRSError(error);
        if (useLegacy) {
            console.warn(
                "Please work to migrate all non RSError throws to use an RSError-extending error type."
            );
        }
        return {
            hasError: true,
            code: useLegacy ? ErrorName.NON_RS_ERROR : error.code,
            type: useLegacy ? "page" : error.displayAs,
        };
    }
    /** Any developer logging needed (i.e. log the error in console, push to
     * analytics, etc.) */
    componentDidCatch(error: RSError, errorInfo: ErrorInfo) {
        console.error(error, errorInfo);
    }
    /** Renders the right error page for the right error type, OR the wrapped
     * component if no error is thrown */
    render() {
        if (this.state.hasError) {
            return <ErrorPage code={this.state.code} type={this.state.type} />;
        }
        return this.props.children;
    }
}
/** Just a nice way to keep individual uses of RSErrorBoundary out of jsx since it
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
/** Use in exports and JSX calls to wrap an element in an error boundary and suspense
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
    return withThrowableError(
        <Suspense fallback={<Spinner />}>{component}</Suspense>
    );
};
