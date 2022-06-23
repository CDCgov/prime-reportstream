import { FC, PropsWithChildren, ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { OktaAuth } from "@okta/okta-auth-js";

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

const SessionWrapper =
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

export * from "@testing-library/react";
export { renderWithRouter };
export { renderWithSession };
