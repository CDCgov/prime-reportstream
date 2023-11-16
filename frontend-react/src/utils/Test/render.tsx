import React, { ReactElement } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { CacheProvider } from "rest-hooks";
import { PartialDeep } from "type-fest";
import { ErrorBoundary, FallbackProps } from "react-error-boundary";
import { Fixture, MockResolver } from "@rest-hooks/test";
import {
    RenderHookOptions,
    RenderOptions,
    Queries,
    queries,
    render as _render,
    renderHook as _renderHook,
} from "@testing-library/react";

import { AuthorizedFetchProvider } from "../../contexts/AuthorizedFetch";
import { getTestQueryClient } from "../../network/QueryClients";
import { useSessionContext } from "../../contexts/Session";
import { useAppInsightsContext } from "../../contexts/AppInsights";
import { useFeatureFlags } from "../../contexts/FeatureFlags";
import { useToast } from "../../contexts/Toast";

function TestError({ error }: FallbackProps) {
    return <>{error.toString()}</>;
}

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
type AppWrapperProviderHooks = typeof AppWrapperProviderHooksMap;
type AppWrapperProviderHooksOptions = Partial<{
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
                                FallbackComponent={TestError}
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

export function render(
    ui: ReactElement,
    {
        restHookFixtures,
        providers,
        onError,
        wrapper: _wrapper,
        ...options
    }: RenderAppOptions = {},
) {
    return _render(ui, {
        wrapper:
            _wrapper ??
            AppWrapper({
                restHookFixtures,
                providers,
                onError,
            }),
        ...options,
    });
}

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
        onError,
        wrapper: _wrapper,
        ...options
    }: RenderHookOptions<Props, Q, Container, BaseElement> &
        AppWrapperOptions = {},
) {
    return _renderHook<Result, Props, Q, Container, BaseElement>(render, {
        wrapper:
            _wrapper ??
            AppWrapper({
                providers,
                restHookFixtures,
                onError,
            }),
        ...options,
    });
}

export type RSRender = typeof render;
export type RSRenderHook = typeof renderHook;

export { screen, act, waitFor } from "@testing-library/react";
