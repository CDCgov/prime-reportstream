import { render } from "@testing-library/react";

import App from "./App";
import type { AppConfig } from "./config";
import { createTelemetryService } from "./TelemetryService";
import { isUseragentPreferred } from "./utils/BrowserUtils";

function MockComponent({ children }: any) {
    return <>{children}</>;
}

jest.mock("rest-hooks", () => {
    return {
        __esModule: true,
        NetworkErrorBoundary: MockComponent,
        CacheProvider: MockComponent,
    };
});
jest.mock("@tanstack/react-query", () => {
    return {
        __esModule: true,
        QueryClientProvider: MockComponent,
    };
});
jest.mock("@tanstack/react-query-devtools");
jest.mock("react-helmet-async", () => {
    return {
        __esModule: true,
        HelmetProvider: MockComponent,
    };
});
jest.mock("@okta/okta-auth-js");
jest.mock("./pages/error/ErrorPage");
jest.mock("./contexts/AuthorizedFetch", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
jest.mock("./contexts/FeatureFlag", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
jest.mock("./contexts/Session", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
jest.mock("./network/QueryClients", () => {
    return {
        __esModule: true,
        appQueryClient: {},
    };
});
jest.mock("./TelemetryService")
jest.mock("./shared/DAPScript/DAPScript");
jest.mock("./config");
jest.mock("./contexts/Toast", () => {
    return {
        __esModule: true,
        default: MockComponent,
    };
});
jest.mock("./utils/BrowserUtils", () => {
    return {
        __esModule: true,
        isUseragentPreferred: jest.fn(),
    };
});
jest.mock("react-router-dom", () => {
    return {
        createBrowserRouter: jest.fn(),
        RouterProvider: MockComponent
    }
})
jest.mock("./components/RSErrorBoundary", () => {
    return {
        __esModule: true,
        default: MockComponent
    }
})

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
        onIdle: jest.fn(),
    },
    MODE: "test",
    APPLICATION_INSIGHTS: {} as any,
    OKTA_AUTH: {} as any,
    OKTA_WIDGET: {} as any,
} as const satisfies AppConfig;

const mockIsUseragentPreferred = jest.mocked(isUseragentPreferred);
const mockCreateTelemetryService = jest.mocked(createTelemetryService);
const {reactPlugin: mockReactPlugin} = mockCreateTelemetryService({});

function setup(isUseragentPreferred = true) {
    mockIsUseragentPreferred.mockReturnValue(isUseragentPreferred);

    render(<App config={config} routes={[]} />);
}

describe("App component", () => {
    describe("telemetry custom properties", () => {
        describe("isUserAgentOutdated", () => {
            test("undefined when regex test passes", () => {
                setup();
                expect(mockReactPlugin.customProperties.isUserAgentOutdated).toBe(undefined)
            });

            test("true when test fails", () => {
                setup(false);
                expect(mockReactPlugin.customProperties.isUserAgentOutdated).toBe(true)
            });
        });
    });
});
