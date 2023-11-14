import { ReactElement } from "react";
import {
    render as renderOrig,
    RenderOptions,
    renderHook as renderHookOrig,
    RenderHookOptions,
    Queries,
    queries,
} from "@testing-library/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { CacheProvider } from "rest-hooks";
import { PartialDeep } from "type-fest";
import { ErrorBoundary } from "react-error-boundary";
import { LinkProps } from "react-router-dom";
import { Fixture, MockResolver } from "@rest-hooks/test";

import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetch";
import { getTestQueryClient } from "../network/QueryClients";
import { useSessionContext } from "../contexts/Session";
import { useAppInsightsContext } from "../contexts/AppInsights";
import { useFeatureFlags } from "../contexts/FeatureFlags";
import { useToast } from "../contexts/Toast";

const MockLinkBase = ({
    to,
    className,
    state: _state,
    ...props
}: LinkProps) => (
    <a
        className={typeof className === "function" ? className({}) : className}
        href={to}
        {...props}
    />
);
const MockLink = vi.fn(MockLinkBase);
MockLink.displayName = "Link";

vi.mock("react-router-dom", async (imp) => ({
    ...(await imp()),
    useMatch: vi.fn(),
    useNavigation: vi.fn(),
    useHref: vi.fn(),
    useRoutes: vi.fn(),
    useNavigate: vi.fn(),
    useLocation: vi.fn(() => window.location),
    useParams: vi.fn(() => ({})),
    useMatches: vi.fn(() => []),
    useSearchParams: vi.fn(),
    useResolvedPath: vi.fn(),
    useLoaderData: vi.fn(),
    useFetcher: vi.fn(),
    useOutlet: vi.fn(),
    useOutletContext: vi.fn(),
    useRouteLoaderData: vi.fn(),
    useSubmit: vi.fn(),
    useNavigateType: vi.fn(),
    useInRouterContext: vi.fn(),
    useLinkClickHandler: vi.fn(),
    useLinkPressHandler: vi.fn(),
    useActionData: vi.fn(),
    Link: MockLink,
    NavLink: vi.fn(MockLink),
}));

interface AppWrapperProps {
    children: React.ReactNode;
}

interface AppWrapperOptions {
    restHookFixtures?: Fixture[];
    providers?: AppWrapperProviderHooksOptions;
    onError?: (...any: any[]) => void;
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
    onError,
    restHookFixtures,
    providers = {},
}: AppWrapperOptions = {}) => {
    for (const [k, v] of Object.entries(providers) as [
        keyof AppWrapperProviderHooks,
        any,
    ][]) {
        AppWrapperProviderHooksMap[k].mockReturnValue(v);
    }
    return ({ children }: AppWrapperProps) => {
        return (
            <CacheProvider>
                <HelmetProvider>
                    <QueryClientProvider client={getTestQueryClient()}>
                        <AuthorizedFetchProvider>
                            <ErrorBoundary
                                onError={(e) => onError?.(e)}
                                fallbackRender={(props) => (
                                    <>{props.error.toString()}</>
                                )}
                            >
                                {restHookFixtures ? (
                                    <MockResolver fixtures={restHookFixtures}>
                                        {children}
                                    </MockResolver>
                                ) : (
                                    children
                                )}
                            </ErrorBoundary>
                        </AuthorizedFetchProvider>
                    </QueryClientProvider>
                </HelmetProvider>
            </CacheProvider>
        );
    };
};

interface RenderAppOptions extends RenderOptions, AppWrapperOptions {}

export const render = (
    ui: ReactElement,
    {
        restHookFixtures,
        providers,
        onError,
        ...options
    }: Omit<RenderAppOptions, "wrapper"> = {},
) => {
    return renderOrig(ui, {
        wrapper: AppWrapper({
            restHookFixtures,
            providers,
            onError,
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
        restHookFixtures,
        providers,
        ...options
    }: RenderHookOptions<Props, Q, Container, BaseElement> &
        AppWrapperOptions = {},
) {
    return renderHookOrig<Result, Props, Q, Container, BaseElement>(render, {
        wrapper: AppWrapper({
            providers,
            restHookFixtures,
        }),
        ...options,
    });
}

export type RSRender = typeof render;
export type RSRenderHook = typeof renderHook;
