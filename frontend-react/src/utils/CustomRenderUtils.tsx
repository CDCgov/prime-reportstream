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
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { Fixture, MockResolver } from "@rest-hooks/test";
import { CacheProvider } from "rest-hooks";

import {
    SessionProviderBase,
    SessionProviderBaseProps,
} from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { FeatureFlagProvider } from "../contexts/FeatureFlagContext";
import { appRoutes } from "../AppRouter";
import AppInsightsContextProvider, {
    AppInsightsContextProviderProps,
} from "../contexts/AppInsightsContext";

//vi.mock("../../../contexts/AppInsightsContext");
//vi.mock("../../../contexts/SessionContext");
//vi.mock("@tanstack/react-query");

interface AppWrapperProps {
    children: React.ReactNode;
}

interface AppWrapperOptions {
    initialRouteEntries?: string[];
    restHookFixtures?: Fixture[];
    providers?: {
        cache?: boolean;
        helmet?: boolean;
        appInsights?: boolean | AppInsightsContextProviderProps;
        session?: boolean | SessionProviderBaseProps;
        queryClient?: boolean;
        authorizedFetch?: boolean;
        featureFlag?: boolean;
        router?: boolean;
    };
}

export interface TestProviderProps extends React.PropsWithChildren {
    Provider: React.ComponentType<any>;
    boolOrProps?: boolean | object;
}
function TestProvider({ Provider, boolOrProps, children }: TestProviderProps) {
    if (!boolOrProps) return <>{children}</>;

    return (
        <Provider {...(typeof boolOrProps !== "boolean" ? boolOrProps : {})}>
            {children}
        </Provider>
    ) as JSX.Element;
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
    providers: {
        cache,
        appInsights,
        authorizedFetch,
        helmet,
        queryClient,
        session,
        featureFlag,
        router,
    } = {},
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
        const queryClientProps = queryClient ?? {
            client: new QueryClient({
                defaultOptions: {
                    queries: { retry: false },
                },
            }),
        };
        const routerProps =
            router || initialRouteEntries
                ? {
                      router: createMemoryRouter(
                          createTestRoutes(appRoutes, children),
                          {
                              initialEntries: initialRouteEntries,
                          },
                      ),
                  }
                : false;
        return (
            <TestProvider Provider={CacheProvider} boolOrProps={cache}>
                <TestProvider Provider={HelmetProvider} boolOrProps={helmet}>
                    <TestProvider
                        Provider={AppInsightsContextProvider}
                        boolOrProps={appInsights}
                    >
                        <TestProvider
                            Provider={SessionProviderBase}
                            boolOrProps={session}
                        >
                            <TestProvider
                                Provider={QueryClientProvider}
                                boolOrProps={queryClientProps}
                            >
                                <TestProvider
                                    Provider={AuthorizedFetchProvider}
                                    boolOrProps={authorizedFetch}
                                >
                                    <TestProvider
                                        Provider={FeatureFlagProvider}
                                        boolOrProps={featureFlag}
                                    >
                                        <TestProvider
                                            Provider={MockResolver}
                                            boolOrProps={
                                                restHookFixtures && {
                                                    fixtures: {
                                                        restHookFixtures,
                                                    },
                                                }
                                            }
                                        >
                                            {routerProps ? (
                                                <TestProvider
                                                    Provider={RouterProvider}
                                                    boolOrProps={routerProps}
                                                />
                                            ) : (
                                                children
                                            )}
                                        </TestProvider>
                                    </TestProvider>
                                </TestProvider>
                            </TestProvider>
                        </TestProvider>
                    </TestProvider>
                </TestProvider>
            </TestProvider>
        );
    };
};

interface RenderAppOptions extends Omit<RenderOptions, "wrapper"> {
    appWrapper?: AppWrapperOptions;
}

export const renderApp = (
    ui: ReactElement,
    { appWrapper, ...options }: RenderAppOptions = {},
) => {
    return render(ui, {
        wrapper: AppWrapper(appWrapper),
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
        appWrapper,
        ...options
    }: RenderHookOptions<Props, Q, Container, BaseElement> & {
        appWrapper?: AppWrapperOptions;
    } = {},
) {
    return renderHookOrig<Result, Props, Q, Container, BaseElement>(render, {
        wrapper: AppWrapper(appWrapper),
        ...options,
    });
}

export { screen } from "@testing-library/react";
