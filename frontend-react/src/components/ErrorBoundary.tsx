import React, { ReactNode } from 'react'
import { ErrorPage } from '../pages/error/ErrorPage'

interface EventBoundaryProps {
    children?: JSX.Element[],
    fallback: JSX.Element
}

interface EventBoundaryState {
    hasError: boolean,
}

export class ErrorBoundary extends React.Component<EventBoundaryProps, EventBoundaryState> {
    constructor(props: EventBoundaryProps) {
        super(props);
        this.state = {
            hasError: false,
        };
    }

    static getDerivedStateFromError(error: any): EventBoundaryState {
        if (error) { return { hasError: true, } }
        return { hasError: false }
    }

    componentDidCatch(error: any, errorInfo: React.ErrorInfo) {
        console.log(`${error}: ${errorInfo}`);
    }

    render(): ReactNode {
        if (this.state.hasError && this.props.fallback) {
            return this.props.fallback
        } else if (this.state.hasError) {
            return <ErrorPage />
        }
        return this.props.children
    }
}

export default ErrorBoundary
