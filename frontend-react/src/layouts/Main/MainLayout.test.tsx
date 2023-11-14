import { screen } from "@testing-library/react";

import MainLayout from "./MainLayout";

function ErroringComponent() {
    throw new Error("Test");
    // eslint-disable-next-line no-unreachable
    return <></>;
}

describe("MainLayout", () => {
    test("Renders children", () => {
        render(<MainLayout>Test</MainLayout>);
        expect(screen.getByRole("main")).toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Test");
    });

    test("Renders error", () => {
        const mockOnError = vi.fn();
        render(
            <MainLayout ErrorBoundary={((props: any) => props.children) as any}>
                <ErroringComponent />
            </MainLayout>,
            { onError: mockOnError },
        );
        expect(mockOnError).toBeCalled();
    });
});
