import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import MainLayout from "./MainLayout";

function ErroringComponent() {
    throw new Error("Test");
    // eslint-disable-next-line no-unreachable
    return <></>;
}

describe("MainLayout", () => {
    test("Renders children", () => {
        renderApp(<MainLayout>Test</MainLayout>);
        expect(screen.getByRole("main")).toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });

    test("Renders error", () => {
        renderApp(
            <MainLayout>
                <ErroringComponent />
            </MainLayout>,
        );
        expect(screen.getByRole("main")).toBeInTheDocument();
        expect(screen.getByRole("alert")).toBeInTheDocument();
    });
});
