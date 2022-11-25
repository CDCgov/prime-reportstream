import { FC, PropsWithChildren, ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { OktaAuth } from "@okta/okta-auth-js";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import SessionProvider, { OktaHook } from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { testQueryClient } from "../network/QueryClients";
import { FeatureFlagProvider } from "../contexts/FeatureFlagContext";

import { mockToken } from "./TestUtils";

/* Use this to generate fake useOktaAuth() hooks to pass into renderWithSession
 * This serves as our way of mocking different token, auth, and claims values */
export const makeOktaHook = (_init?: Partial<IOktaContext>): OktaHook => {
    return () => ({
        authState: {
            accessToken: mockToken(),
            ..._init?.authState,
        },
        oktaAuth: {
            ..._init?.oktaAuth,
        } as OktaAuth,
    });
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
const RouterWrapper: FC = ({ children }) => {
    return <BrowserRouter>{children}</BrowserRouter>;
};

export const SessionWrapper =
    (mockOkta: OktaHook) =>
    ({ children }: PropsWithChildren<{}>) => {
        return (
            <RouterWrapper>
                <SessionProvider oktaHook={mockOkta}>
                    {children}
                </SessionProvider>
            </RouterWrapper>
        );
    };

export const QueryWrapper =
    (client: QueryClient = new QueryClient()) =>
    ({ children }: PropsWithChildren<{}>) =>
        (
            <QueryClientProvider client={client}>
                <AuthorizedFetchProvider initializedOverride={true}>
                    {children}
                </AuthorizedFetchProvider>
            </QueryClientProvider>
        );

const FeatureFlagWrapper: FC = ({ children }) => {
    return <FeatureFlagProvider>{children}</FeatureFlagProvider>;
};

const AppWrapper =
    (mockOkta: OktaHook) =>
    ({ children }: PropsWithChildren<{}>) => {
        return (
            <RouterWrapper>
                <SessionProvider oktaHook={mockOkta}>
                    <QueryClientProvider client={testQueryClient}>
                        <AuthorizedFetchProvider>
                            <FeatureFlagProvider>
                                {children}
                            </FeatureFlagProvider>
                        </AuthorizedFetchProvider>
                    </QueryClientProvider>
                </SessionProvider>
            </RouterWrapper>
        );
    };

const renderWithRouter = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) => render(ui, { wrapper: RouterWrapper, ...options });

const renderWithSession = (
    ui: ReactElement,
    oktaHook?: OktaHook,
    options?: Omit<RenderOptions, "wrapper">
) =>
    render(ui, {
        wrapper: SessionWrapper(oktaHook || makeOktaHook()),
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
        wrapper: AppWrapper(oktaHook || makeOktaHook()),
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
