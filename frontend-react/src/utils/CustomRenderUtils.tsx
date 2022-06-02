import { FC, ReactElement, useReducer } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import useSessionStorage from "../hooks/UseSessionStorage";
import { SessionContext } from "../contexts/SessionStorageContext";

// import SessionProvider from "../contexts/SessionStorageContext";

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

const SessionWrapper: FC = ({ children }) => {
    const [fakeMemberState, fakeMemberDispatch] = useReducer(() => ({}), {});
    const store = useSessionStorage();

    const FakeSessionProvider: FC = () => (
        <SessionContext.Provider
            value={{
                memberships: {
                    state: fakeMemberState,
                    dispatch: fakeMemberDispatch,
                },
                store,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
    return (
        <RouterWrapper>
            <FakeSessionProvider>{children}</FakeSessionProvider>
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
