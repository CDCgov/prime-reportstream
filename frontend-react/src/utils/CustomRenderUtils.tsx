import { PropsWithChildren, ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { OktaAuth } from "@okta/okta-auth-js";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";

import SessionProvider, { OktaHook } from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { getTestQueryClient } from "../network/QueryClients";
import { FeatureFlagProvider } from "../contexts/FeatureFlagContext";

import { mockToken } from "./TestUtils";

/* Use this to generate fake useOktaAuth() hooks to pass into renderWithSession
 * This serves as our way of mocking different token, auth, and claims values */
export const makeOktaHook = (_init?: Partial<IOktaContext>): OktaHook => {
    const result = {
        authState: {
            accessToken: mockToken(),
            ..._init?.authState,
        },
        oktaAuth: {
            ..._init?.oktaAuth,
        } as OktaAuth,
    };
    return () => result;
};

/*
    To create a custom renderer, you must create a functional
    component and a custom render function.

    @see: https://testing-library.com/docs/react-testing-library/setup/#custom-render
*/

/*
    Use `renderWithRouter()` from this module as our standard
    renderer across most tests. This will prevent hitting the
    React error when rendering for unit tests.
*/
const RouterWrapper = ({ children }: PropsWithChildren<{}>) => {
    return (
        <BaseWrapper>
            <BrowserRouter>{children}</BrowserRouter>
        </BaseWrapper>
    );
};

export const SessionWrapper = ({ children }: PropsWithChildren<{}>) => {
    return (
        <BaseWrapper>
            <RouterWrapper>
                <SessionProvider>{children}</SessionProvider>
            </RouterWrapper>
        </BaseWrapper>
    );
};

export const QueryWrapper =
    (client: QueryClient = getTestQueryClient()) =>
    ({ children }: PropsWithChildren<{}>) =>
        (
            <BaseWrapper>
                <QueryClientProvider client={client}>
                    <AuthorizedFetchProvider initializedOverride={true}>
                        {children}
                    </AuthorizedFetchProvider>
                </QueryClientProvider>
            </BaseWrapper>
        );

export const BaseWrapper = ({ children }: PropsWithChildren<{}>) => (
    <HelmetProvider>{children}</HelmetProvider>
);
const FeatureFlagWrapper = ({ children }: PropsWithChildren<{}>) => {
    return (
        <BaseWrapper>
            <FeatureFlagProvider>{children}</FeatureFlagProvider>
        </BaseWrapper>
    );
};

const AppWrapper = ({ children }: PropsWithChildren<{}>) => {
    return (
        <BaseWrapper>
            <RouterWrapper>
                <SessionProvider>
                    <QueryClientProvider client={getTestQueryClient()}>
                        <AuthorizedFetchProvider>
                            <FeatureFlagProvider>
                                {children}
                            </FeatureFlagProvider>
                        </AuthorizedFetchProvider>
                    </QueryClientProvider>
                </SessionProvider>
            </RouterWrapper>
        </BaseWrapper>
    );
};

export const renderWithBase = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) =>
    render(ui, {
        wrapper: BaseWrapper,
        ...options,
    });

const renderWithRouter = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) => render(ui, { wrapper: RouterWrapper, ...options });

const renderWithSession = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) =>
    render(ui, {
        wrapper: SessionWrapper,
        ...options,
    });

// render an element with parent wrapper other than a div
// used to silence testing library errors when wrapping elements
// that should not be children of divs (such as <td> etc)
export const renderWithCustomWrapper = (
    ui: ReactElement,
    wrapperType: string
) => {
    return render(ui, {
        container: document.body.appendChild(
            document.createElement(wrapperType)
        ),
    });
};

// render wrapped with BrowserRouter, SessionProvider, and QueryClientProvider
export const renderWithFullAppContext = (
    ui: ReactElement,
    oktaHook?: OktaHook,
    options?: Omit<RenderOptions, "wrapper">
) => {
    return render(ui, {
        wrapper: AppWrapper,
        ...options,
    });
};

// for testing components that need access to react-query
export const renderWithQueryProvider = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) =>
    render(ui, {
        wrapper: QueryWrapper(),
        ...options,
    });

// for testing components that need access to feature flags
export const renderWithFeatureFlags = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) =>
    render(ui, {
        wrapper: FeatureFlagWrapper,
        ...options,
    });

export * from "@testing-library/react";
export { renderWithRouter };
export { renderWithSession };
