import { Fixture, MockResolver } from "@rest-hooks/test";
import { QueryClientProvider } from "@tanstack/react-query";
import {
    queries,
    Queries,
    render,
    RenderHookOptions,
    renderHook as renderHookOrig,
    RenderOptions,
} from "@testing-library/react";
import { ReactElement, ReactNode, Suspense } from "react";
import { HelmetProvider } from "react-helmet-async";
import {
    createMemoryRouter,
    Outlet,
    RouteObject,
    RouterProvider,
} from "react-router-dom";
import { CacheProvider } from "rest-hooks";

import { appRoutes } from "../AppRouter";
import { getTestQueryClient } from "../network/QueryClients";

interface AppWrapperProps {
    children: ReactNode;
}

interface AppWrapperOptions {
    initialRouteEntries?: string[];
    restHookFixtures?: Fixture[];
}

function TestLayout() {
    return <Outlet />;
}

function createTestRoutes(
    routes: RouteObject[],
    element: ReactNode,
): RouteObject[] {
    return routes.map((r) => ({
        ...r,
        lazy: undefined,
        Component: undefined,
        element: r.path !== "/" ? element : <TestLayout />,
        children: r.children
            ? createTestRoutes(r.children, element)
            : undefined,
    })) as RouteObject[];
}
export const AppWrapper = ({
    initialRouteEntries,
    restHookFixtures,
}: AppWrapperOptions = {}) => {
    // FUTURE_TODO: Replace children with <AppRouter /> if initialRouteEntries after mocking okta users
    // in tests is made easier for better coverage as we'd be able to test through
    // any custom route wrappers.
    // FUTURE_TODO: Remove MockResolver and restHookFixtures when removing react-hooks.
    return function InnerAppWrapper({ children }: AppWrapperProps) {
        /**
         * Dynamically makes the supplied children the return element for all
         * routes.
         * FUTURE_TODO: Remove this once okta user/session mocking is easier
         * and use <AppRouter /> instead.
         */
        const router = createMemoryRouter(
            createTestRoutes(appRoutes, children),
            {
                initialEntries: initialRouteEntries,
            },
        );
        return (
            <Suspense>
                <CacheProvider>
                    <HelmetProvider>
                        <QueryClientProvider client={getTestQueryClient()}>
                            {restHookFixtures ? (
                                <MockResolver fixtures={restHookFixtures}>
                                    <RouterProvider router={router} />
                                </MockResolver>
                            ) : (
                                <RouterProvider router={router} />
                            )}
                        </QueryClientProvider>
                    </HelmetProvider>
                </CacheProvider>
            </Suspense>
        );
    };
};

interface RenderAppOptions extends RenderOptions, AppWrapperOptions {}

export const renderApp = (
    ui: ReactElement,
    {
        initialRouteEntries,
        restHookFixtures,
        ...options
    }: Omit<RenderAppOptions, "wrapper"> = {},
) => {
    return render(ui, {
        wrapper: AppWrapper({ initialRouteEntries, restHookFixtures }),
        ...options,
    });
};

export function renderHook<
    Result,
    Props,
    Q extends Queries = typeof queries,
    Container extends Element | DocumentFragment = HTMLElement,
    BaseElement extends Element | DocumentFragment = Container,
>(
    render: (initialProps: Props) => Result,
    options?: RenderHookOptions<Props, Q, Container, BaseElement>,
) {
    return renderHookOrig<Result, Props, Q, Container, BaseElement>(render, {
        wrapper: AppWrapper(),
        ...options,
    });
}

export { screen } from "@testing-library/react";
