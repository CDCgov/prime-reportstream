import { FC, ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";

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

const renderWithRouter = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) => render(ui, { wrapper: RouterWrapper, ...options });

export * from "@testing-library/react";
export { renderWithRouter };
