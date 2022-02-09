import { FC, ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { CacheProvider } from "rest-hooks";

const RouterWrapper: FC = ({ children }) => {
    return (
        <BrowserRouter>
            <CacheProvider>{children}</CacheProvider>
        </BrowserRouter>
    );
};

const renderWithRouter = (
    ui: ReactElement,
    options?: Omit<RenderOptions, "wrapper">
) => render(ui, { wrapper: RouterWrapper, ...options });

export * from "@testing-library/react";
export { renderWithRouter };
