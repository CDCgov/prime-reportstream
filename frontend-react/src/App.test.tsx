import { fireEvent, render } from "@testing-library/react";
import { useLocation } from "react-router-dom";

import App from "./App";
import type { AppConfig } from "./config";
import { useAppInsightsContext } from "./contexts/AppInsights";
import { useSessionContext } from "./contexts/Session";
import { isUseragentPreferred } from "./utils/BrowserUtils";

function Layout() {
    return <section>Layout</section>;
}

function MockComponent({ children }: any) {
    return <>{children}</>;
}

vi.mock("rest-hooks", () => {
    return {
        __esModule: true,
        NetworkErrorBoundary: MockComponent,
        CacheProvider: MockComponent,
    };
});
vi.mock("react-router-dom", () => {
    return {
        __esModule: true,
        useNavigate: vi.fn(),
        useLocation: vi.fn(),
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
vi.mock("./contexts/FeatureFlag", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
vi.mock("./contexts/Session", () => {
    return {
        __esModule: true,
        default: MockComponent,
        useSessionContext: vi.fn(),
    };
});
vi.mock("./network/QueryClients", () => {
    return {
        __esModule: true,
        appQueryClient: {},
    };
});
vi.mock("./contexts/AppInsights", () => {
    return {
        __esModule: true,
        useAppInsightsContext: vi.fn(),
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

const config = {
    AI_CONSOLE_SEVERITY_LEVELS: {} as any,
    AI_REPORTABLE_CONSOLE_LEVELS: [],
    API_ROOT: "" as any,
    DEFAULT_FEATURE_FLAGS: "" as any,
    IS_PREVIEW: false,
    OKTA_CLIENT_ID: "",
    OKTA_URL: "",
    RS_API_URL: "",
    IDLE_TIMERS: {
        timeout: 1000 * 60 * 15,
        debounce: 500,
        onIdle: vi.fn(),
    },
    MODE: "test",
    PAGE_META: {
        defaults: {
            title: "test title",
            description: "test description",
            openGraph: {
                image: {
                    src: "",
                    altText: "",
                },
            },
        },
    },
} as const satisfies AppConfig;

const mockUseSessionContext = vi.mocked(useSessionContext);
const mockUseLocation = vi.mocked(useLocation);
const mockUseAppInsightsContext = vi.mocked(useAppInsightsContext);
const mockLogout = vi.fn();
const mockIsAuthenticated = vi.fn().mockReturnValue(true);
const mockOnIdle = config.IDLE_TIMERS.onIdle;
const mockSetTelemetryCustomProperty = vi.fn();
const mockIsUseragentPreferred = vi.mocked(isUseragentPreferred);

const sessionCtx = {
    oktaAuth: {
        isAuthenticated: mockIsAuthenticated,
    },
    authState: {},
    logout: mockLogout,
    activeMembership: {},
    config,
};

mockUseSessionContext.mockReturnValue(sessionCtx as any);
mockUseLocation.mockReturnValue({
    pathname: "/",
} as any);
mockUseAppInsightsContext.mockReturnValue({
    setTelemetryCustomProperty: mockSetTelemetryCustomProperty,
} as any);

function setup(isUseragentPreferred = true) {
    mockIsUseragentPreferred.mockReturnValue(isUseragentPreferred);

    render(
        <App
            Layout={Layout}
            config={sessionCtx.config}
            oktaAuth={sessionCtx.oktaAuth as any}
        />,
    );
}

describe("App component", () => {
    describe("telemetry custom properties", () => {
        test("activeMembership", () => {
            setup();
            expect(mockSetTelemetryCustomProperty).toHaveBeenCalledWith(
                "activeMembership",
                sessionCtx.activeMembership,
            );
        });
        describe("isUserAgentOutdated", () => {
            test("undefined when regex test passes", () => {
                setup();
                expect(mockSetTelemetryCustomProperty).toHaveBeenCalledWith(
                    "isUserAgentOutdated",
                    undefined,
                );
            });

            test("true when test fails", () => {
                setup(false);
                expect(mockSetTelemetryCustomProperty).toHaveBeenCalledWith(
                    "isUserAgentOutdated",
                    true,
                );
            });
        });
    });

    describe("idle timer", () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });
        afterEach(() => {
            vi.useRealTimers();
        });
        test("Idle timer does not trigger before configured time", () => {
            const testPeriods = [
                config.IDLE_TIMERS.timeout / 3,
                (config.IDLE_TIMERS.timeout / 3) * 2,
                config.IDLE_TIMERS.timeout - 1000 * 60,
            ];
            const start = Date.now();
            setup();

            expect(mockOnIdle).not.toHaveBeenCalled();

            for (const timePeriod of testPeriods) {
                vi.setSystemTime(start + timePeriod);
                fireEvent.focus(document);
                expect(mockOnIdle).not.toHaveBeenCalled();
            }
        });

        test("Idle timer triggers at configured time", () => {
            const start = Date.now();
            setup();

            expect(mockOnIdle).not.toHaveBeenCalled();
            vi.setSystemTime(start + config.IDLE_TIMERS.timeout);
            fireEvent.focus(document);
            expect(mockOnIdle).toHaveBeenCalled();
        });
    });
});
