import { SeverityLevel } from "@microsoft/applicationinsights-web";
import { RSSessionContext } from "./SessionProvider";
import { AppConfig } from "../../config";
import { mockRsconsole } from "../../utils/rsConsole/rsConsole.fixtures";

export const configFixture = {
    RSCONSOLE: {
        severityLevels: {
            info: SeverityLevel.Information,
            warn: SeverityLevel.Warning,
            error: SeverityLevel.Error,
            debug: SeverityLevel.Verbose,
            assert: SeverityLevel.Error,
            trace: SeverityLevel.Warning,
        },
        reportableLevels: [],
    },
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
    APPLICATION_INSIGHTS: {
        connectionString: "instrumentationKey=test",
    } as any,
    OKTA_AUTH: {} as any,
    OKTA_WIDGET: {} as any,
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

export const contextFixture = {
    oktaAuth: {
        isAuthenticated: vi.fn(),
    } as any,
    authState: {},
    logout: vi.fn(),
    activeMembership: {} as any,
    user: {
        isUserAdmin: false,
        isUserSender: false,
        isUserReceiver: false,
    } as any,
    setActiveMembership: vi.fn(),
    config: configFixture,
    site: {} as any,
    rsConsole: mockRsconsole as any,
} satisfies RSSessionContext;
