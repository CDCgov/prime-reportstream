import { render, screen } from "@testing-library/react";
import { PropsWithChildren } from "react";
import App from "./App";
import { configFixture } from "./contexts/Session/useSessionContext.fixtures";

function MockComponent({ children }: any) {
    return <div data-testid="app">{children}</div>;
}

vi.mock("rest-hooks", () => {
    return {
        __esModule: true,
        NetworkErrorBoundary: MockComponent,
        CacheProvider: MockComponent,
    };
});
vi.mock("@tanstack/react-query", () => {
    return {
        __esModule: true,
        QueryClientProvider: MockComponent,
    };
});
vi.mock("@tanstack/react-query-devtools");
vi.mock("react-helmet-async", () => {
    return {
        __esModule: true,
        HelmetProvider: MockComponent,
    };
});
vi.mock("@okta/okta-auth-js");
vi.mock("./components/ScrollRestoration", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
vi.mock("./hooks/UseScrollToTop");
vi.mock("./utils/PermissionsUtils");
vi.mock("./pages/error/ErrorPage");
vi.mock("./contexts/AuthorizedFetch", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
vi.mock("./network/QueryClients", () => {
    return {
        __esModule: true,
        appQueryClient: {},
    };
});
vi.mock("./shared/DAPScript/DAPScript");
vi.mock("./config");
vi.mock("./contexts/Toast", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
vi.mock("./utils/BrowserUtils", () => {
    return {
        __esModule: true,
        isUseragentPreferred: vi.fn(),
    };
});
vi.mock("react-router-dom", () => {
    return {
        createBrowserRouter: vi.fn(),
        RouterProvider: MockComponent,
    };
});
vi.mock("./components/RSErrorBoundary", () => {
    return {
        AppErrorBoundary: ({ children }: PropsWithChildren) => children,
    };
});

describe("App component", () => {
    test("renders without error", () => {
        render(<App config={configFixture} routes={[]} />);
        expect(screen.getByTestId("app")).toBeInTheDocument();
    });
});
