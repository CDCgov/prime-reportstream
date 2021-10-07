import React, { ReactNode } from "react";

import { ErrorPage } from "../pages/error/ErrorPage";

interface EventBoundaryProps {
    children: JSX.Element[];
    fallback?: JSX.Element;
}

interface EventBoundaryState {
    hasError: boolean;
}

export class ErrorBoundary extends React.Component<
    EventBoundaryProps,
    EventBoundaryState
> {
    constructor(props: EventBoundaryProps) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(error: any): EventBoundaryState {
        /* INFO
           This is where we are handling what happens in the DOM when an error is thrown.
           We have a boolean that turns on/off our ErrorPage. If false, it returns whatever
           nested children are within the <ErrorBoundary /> 
        */
        if (error) {
            return { hasError: true };
        }
        return { hasError: false };
    }

    componentDidCatch(error: any, errorInfo: React.ErrorInfo) {
        /* INFO
           This is where we should be handling error reporting and logging if
           we decide to track these kinds of things.
        */
        console.log(`${error}: ${errorInfo.componentStack}`);
    }

    render(): ReactNode {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }
            return <ErrorPage />;
        }
        return this.props.children;
    }
}

export default ErrorBoundary;
