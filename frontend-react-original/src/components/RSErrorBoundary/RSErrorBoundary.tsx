import {
    Component,
    ComponentType,
    ErrorInfo,
    FunctionComponent,
    PropsWithChildren,
    ReactElement,
    ReactNode,
    Suspense,
} from "react";
import { ErrorBoundaryProps, FallbackProps } from "react-error-boundary";

import RSErrorSessionBoundary from "./RSErrorSessionBoundary";
import RSErrorUnknownBoundary from "./RSErrorUnknownBoundary";
import { ErrorPage } from "../../pages/error/ErrorPage";
import { RSConsole } from "../../utils/rsConsole/rsConsole";
import Spinner from "../Spinner";

export interface RSErrorBoundaryProps extends PropsWithChildren {
    rsConsole?: RSConsole;
    isGlobalConsole?: boolean;

    onError?: (error: Error, info: ErrorInfo) => void;
    onReset?: (
        details:
            | {
                  reason: "imperative-api";
                  args: any[];
              }
            | {
                  reason: "keys";
                  prev: any[] | undefined;
                  next: any[] | undefined;
              },
    ) => void;
    resetKeys?: any[];
    fallback?: ReactElement<unknown, string | FunctionComponent | typeof Component> | null;
    fallbackRender?: (props: FallbackProps) => ReactNode;
    FallbackComponent?: ComponentType<FallbackProps>;
}

const fallbackElement = <ErrorPage type="message" />;

/**
 * Error boundary that redirects to session-based rsConsole if one not directly provided (can
 * be disabled).
 **/
function RSErrorBoundary({ rsConsole, isGlobalConsole, ...props }: RSErrorBoundaryProps) {
    // default fallback
    const boundaryProps = (
        !props.FallbackComponent && !props.fallback && !props.fallbackRender
            ? {
                  ...props,
                  fallback: fallbackElement,
              }
            : props
    ) as ErrorBoundaryProps;

    // Try to get rsConsole from session if not disabled
    if (!rsConsole && !isGlobalConsole) return <RSErrorSessionBoundary {...boundaryProps} />;

    return <RSErrorUnknownBoundary rsConsole={rsConsole} {...boundaryProps} />;
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
export const withCatch = (component: JSX.Element) => <RSErrorBoundary>{component}</RSErrorBoundary>;
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
export const withSuspense = (component: JSX.Element) => <Suspense fallback={<Spinner />}>{component}</Suspense>;
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
