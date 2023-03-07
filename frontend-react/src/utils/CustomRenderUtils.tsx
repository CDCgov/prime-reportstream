import { ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { MemoryRouter, useRoutes } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { Fixture, MockResolver } from "@rest-hooks/test";
import { CacheProvider } from "rest-hooks";

import SessionProvider from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { getTestQueryClient } from "../network/QueryClients";
import { FeatureFlagProvider } from "../contexts/FeatureFlagContext";
import { appRoutes } from "../AppRouter";

interface AppWrapperProps {
    children: React.ReactNode;
}

interface AppWrapperOptions {
    initialRouteEntries?: string[];
    restHookFixtures?: Fixture[];
}

interface TestRouterProps {
    children: React.ReactNode;
    initialEntries?: string[];
}

/**
 * Dynamically makes the supplied children the return element for all
 * routes.
 * FUTURE_TODO: Remove this once okta user/session mocking is easier
 * and use <AppRouter /> instead.
 */
const TestRoutes = ({ children }: TestRouterProps) => {
    const routes = useRoutes(
        appRoutes.map((r) => ({ ...r, element: children }))
    );

    return routes;
};

export const AppWrapper = ({
    initialRouteEntries,
    restHookFixtures,
}: AppWrapperOptions = {}) => {
    // FUTURE_TODO: Replace children with <AppRouter /> if initialRouteEntries after mocking okta users
    // in tests is made easier for better coverage as we'd be able to test through
    // any custom route wrappers.
    // FUTURE_TODO: Remove MockResolver and restHookFixtures when removing react-hooks.
    return ({ children }: AppWrapperProps) => {
        return (
            <CacheProvider>
                <MemoryRouter initialEntries={initialRouteEntries}>
                    <HelmetProvider>
                        <SessionProvider>
                            <QueryClientProvider client={getTestQueryClient()}>
                                <AuthorizedFetchProvider
                                    initializedOverride={true}
                                >
                                    <FeatureFlagProvider>
                                        {restHookFixtures ? (
                                            <MockResolver
                                                fixtures={restHookFixtures}
                                            >
                                                <TestRoutes>
                                                    {children}
                                                </TestRoutes>
                                            </MockResolver>
                                        ) : (
                                            <TestRoutes>{children}</TestRoutes>
                                        )}
                                    </FeatureFlagProvider>
                                </AuthorizedFetchProvider>
                            </QueryClientProvider>
                        </SessionProvider>
                    </HelmetProvider>
                </MemoryRouter>
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
    }: Omit<RenderAppOptions, "wrapper"> = {}
) => {
    return render(ui, {
        wrapper: AppWrapper({ initialRouteEntries, restHookFixtures }),
        ...options,
    });
};

export * from "@testing-library/react";
