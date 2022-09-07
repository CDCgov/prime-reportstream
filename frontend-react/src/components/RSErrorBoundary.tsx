import React, { ErrorInfo, ReactNode } from "react";

import { ErrorUI, isRSError, RSError } from "../utils/RSError";
import { ErrorPage } from "../pages/error/ErrorPage";

/** For consistency, when passing the code prop, please use these values
 * e.g. <ErrorPage code={RSError.NOT_FOUND} /> */
export enum ErrorName {
    // TODO: Update App.tsx to throw on bad browser, wrap with boundary in index.ts?
    UNSUPPORTED_BROWSER = "unsupported-browser",
    UNAUTHORIZED = "unauthorized",
    NOT_FOUND = "not-found",
    // Any error thrown that cannot be parsed by RSError.parseStatus()
    UNKNOWN = "unknown-error",
    // Any error that does not extend the RSError class
    NON_RS_ERROR = "non-rs-error",
}

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
        console.log(error, errorInfo);
    }
    /** Renders the right error page for the right error type, OR the wrapped
     * component if no error is thrown */
    render() {
        if (this.state.hasError) {
            return <ErrorPage code={this.state.code} />;
        }
        return this.props.children;
    }
}
