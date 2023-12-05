import { ReactElement } from "react";
import {
    render,
    RenderOptions,
    renderHook as renderHookOrig,
    RenderHookOptions,
    Queries,
    queries,
} from "@testing-library/react";
import {
    createMemoryRouter,
    Outlet,
    RouterProvider,
    RouteObject,
} from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { Fixture, MockResolver } from "@rest-hooks/test";
import { CacheProvider } from "rest-hooks";

import { SessionProviderBase } from "../contexts/Session";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetch";
import { getTestQueryClient } from "../network/QueryClients";
import { FeatureFlagProvider } from "../contexts/FeatureFlag";
import { appRoutes } from "../AppRouter";
import AppInsightsContextProvider from "../contexts/AppInsights";
import config from "../config";

interface AppWrapperProps {
    children: React.ReactNode;
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
    element: React.ReactNode,
): RouteObject[] {
    return routes.map((r) => ({
        ...r,
        lazy: undefined,
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
    return ({ children }: AppWrapperProps) => {
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
            <CacheProvider>
                <HelmetProvider>
                    <AppInsightsContextProvider>
                        <SessionProviderBase
                            oktaAuth={{} as any}
                            authState={{}}
                            config={config}
                        >
                            <QueryClientProvider client={getTestQueryClient()}>
                                <AuthorizedFetchProvider
                                    initializedOverride={true}
                                >
                                    <FeatureFlagProvider>
                                        {restHookFixtures ? (
                                            <MockResolver
                                                fixtures={restHookFixtures}
                                            >
                                                <RouterProvider
                                                    router={router}
                                                />
                                            </MockResolver>
                                        ) : (
                                            <RouterProvider router={router} />
                                        )}
                                    </FeatureFlagProvider>
                                </AuthorizedFetchProvider>
                            </QueryClientProvider>
                        </SessionProviderBase>
                    </AppInsightsContextProvider>
                </HelmetProvider>
            </CacheProvider>
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
