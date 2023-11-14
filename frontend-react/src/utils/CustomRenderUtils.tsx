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
import { PartialDeep } from "type-fest";

import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetch";
import { getTestQueryClient } from "../network/QueryClients";
import { appRoutes } from "../AppRouter";
import { useSessionContext } from "../contexts/Session";
import { useAppInsightsContext } from "../contexts/AppInsights";
import { useFeatureFlags } from "../contexts/FeatureFlags";
import { useToast } from "../contexts/Toast";

interface AppWrapperProps {
    children: React.ReactNode;
}

interface AppWrapperOptions {
    initialRouteEntries?: string[];
    restHookFixtures?: Fixture[];
    providers?: AppWrapperProviderHooksOptions;
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
        ErrorBoundary: undefined,
        lazy: undefined,
        element: r.path !== "/" ? element : <TestLayout />,
        children: r.children
            ? createTestRoutes(r.children, element)
            : undefined,
    })) as RouteObject[];
}

const AppWrapperProviderHooksMap = {
    Session:
        vi.mocked<() => PartialDeep<ReturnType<typeof useSessionContext>>>(
            useSessionContext,
        ),
    AppInsights: vi.mocked<
        () => PartialDeep<ReturnType<typeof useAppInsightsContext>>
    >(useAppInsightsContext),
    FeatureFlags:
        vi.mocked<() => PartialDeep<ReturnType<typeof useFeatureFlags>>>(
            useFeatureFlags,
        ),
    Toast: vi.mocked<() => PartialDeep<ReturnType<typeof useToast>>>(useToast),
} as const;
export type AppWrapperProviderHooks = typeof AppWrapperProviderHooksMap;
export type AppWrapperProviderHooksOptions = Partial<{
    [k in keyof AppWrapperProviderHooks]: ReturnType<
        AppWrapperProviderHooks[k]
    >;
}>;

export const AppWrapper = ({
    initialRouteEntries,
    restHookFixtures,
    providers = {},
}: AppWrapperOptions = {}) => {
    for (const [k, v] of Object.entries(providers) as [
        keyof AppWrapperProviderHooks,
        any,
    ][]) {
        AppWrapperProviderHooksMap[k].mockReturnValue(v);
    }
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
                    <QueryClientProvider client={getTestQueryClient()}>
                        <AuthorizedFetchProvider>
                            {restHookFixtures ? (
                                <MockResolver fixtures={restHookFixtures}>
                                    <RouterProvider router={router} />
                                </MockResolver>
                            ) : (
                                <RouterProvider router={router} />
                            )}
                        </AuthorizedFetchProvider>
                    </QueryClientProvider>
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
        providers,
        ...options
    }: Omit<RenderAppOptions, "wrapper"> = {},
) => {
    return render(ui, {
        wrapper: AppWrapper({
            initialRouteEntries,
            restHookFixtures,
            providers,
        }),
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
    {
        providers,
        restHookFixtures,
        initialRouteEntries,
        ...options
    }: RenderHookOptions<Props, Q, Container, BaseElement> &
        AppWrapperOptions = {},
) {
    return renderHookOrig<Result, Props, Q, Container, BaseElement>(render, {
        wrapper: AppWrapper({
            providers,
            restHookFixtures,
            initialRouteEntries,
        }),
        ...options,
    });
}

export { screen } from "@testing-library/react";
