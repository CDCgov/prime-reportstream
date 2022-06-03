import { FC, ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import SessionProvider, { OktaHook } from "../contexts/SessionContext";

import { mockToken } from "./TestUtils";

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

// this, for now, is a static mock function for `useOktaAuth`
// TODO: make this mock dynamic so that we can customize return values
// for use in a wider variety of test scenarios.
// one possibility would be to allow options to pass from renderWithSession into
// a wrapper function that could customize the behavior of the mock within the SesssionWrapper
const mockUseOktaAuth: OktaHook = () => ({
    authState: {
        accessToken: mockToken(),
    },
    oktaAuth: {} as IOktaContext["oktaAuth"],
});

const SessionWrapper: FC = ({ children }) => {
    return (
        <RouterWrapper>
            <SessionProvider oktaHook={mockUseOktaAuth}>
                {children}
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
    options?: Omit<RenderOptions, "wrapper">
) => render(ui, { wrapper: SessionWrapper, ...options });

export * from "@testing-library/react";
export { renderWithRouter };
export { renderWithSession };
