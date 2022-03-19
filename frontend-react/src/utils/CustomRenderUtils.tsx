import { FC, ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";

import OrgProvider from "../contexts/OrgContext";
import { dummyPayload } from "../contexts/OrgContext.test";

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

const OrgContextWrapper: FC = ({ children }) => {
    return <OrgProvider value={dummyPayload}>{children}</OrgProvider>;
};

const renderWithRouter = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) => render(ui, { wrapper: RouterWrapper, ...options });

const renderWithOrgContext = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) => render(ui, { wrapper: OrgContextWrapper, ...options });

export * from "@testing-library/react";
export { renderWithRouter };
export { renderWithOrgContext };
