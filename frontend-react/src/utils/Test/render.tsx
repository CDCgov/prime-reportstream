import React, { ReactElement } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
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

function ConditionalWrapper({
    children,
    Wrapper,
    isEnabled,
}: React.PropsWithChildren<{
    Wrapper: React.ComponentType<React.PropsWithChildren>;
    isEnabled?: boolean;
}>) {
    return isEnabled ? <Wrapper>{children}</Wrapper> : children;
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
}> & {
    QueryClient?: boolean;
};

export const AppWrapper = ({
    onError,
    restHookFixtures,
    providers: { QueryClient, ...hookOptions } = {},
}: AppWrapperOptions = {}) => {
    for (const [k, v] of Object.entries(hookOptions) as [
        keyof AppWrapperProviderHooks,
        any,
    ][]) {
        AppWrapperProviderHooksMap[k].mockReturnValue(v);
    }
    const isQueryClientEnabled = !!QueryClient;
    const isMockResolverEnabled = !!restHookFixtures;
    const isAuthorizedFetchEnabled =
        isMockResolverEnabled || isQueryClientEnabled;

    return ({ children }: AppWrapperProps) => {
        return (
            <ConditionalWrapper
                isEnabled={isMockResolverEnabled}
                Wrapper={({ children }) => (
                    <CacheProvider>{children}</CacheProvider>
                )}
            >
                <ConditionalWrapper
                    Wrapper={({ children }) => (
                        <QueryClientProvider client={getTestQueryClient()}>
                            {children}
                        </QueryClientProvider>
                    )}
                    isEnabled={isQueryClientEnabled}
                >
                    <ConditionalWrapper
                        isEnabled={isAuthorizedFetchEnabled}
                        Wrapper={({ children }) => (
                            <AuthorizedFetchProvider>
                                {children}
                            </AuthorizedFetchProvider>
                        )}
                    >
                        <ErrorBoundary
                            onError={(e) => onError?.(e)}
                            FallbackComponent={TestError}
                        >
                            <ConditionalWrapper
                                isEnabled={isMockResolverEnabled}
                                Wrapper={({ children }) => (
                                    <MockResolver fixtures={restHookFixtures!!}>
                                        {children}
                                    </MockResolver>
                                )}
                            >
                                {children}
                            </ConditionalWrapper>
                        </ErrorBoundary>
                    </ConditionalWrapper>
                </ConditionalWrapper>
            </ConditionalWrapper>
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
