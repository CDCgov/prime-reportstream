import { screen } from "@testing-library/react";

import MainLayout from "./MainLayout";
import { renderApp } from "../../utils/CustomRenderUtils";

function ErroringComponent() {
    throw new Error("Test");

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
