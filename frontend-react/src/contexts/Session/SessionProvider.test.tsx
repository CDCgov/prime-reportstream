import { useOktaAuth } from "@okta/okta-react";
import { fireEvent, render, screen } from "@testing-library/react";

import { useContext } from "react";
import SessionProvider, { SessionContext } from "./SessionProvider";
import { AppConfig } from "../../config";
import { MemberType } from "../../utils/OrganizationUtils";

jest.mock("../../utils/PermissionsUtils");

const mockUseOktaAuth = jest.mocked(useOktaAuth);

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

const mockOnIdle = config.IDLE_TIMERS.onIdle;

describe("SessionContext", () => {
    describe("SessionContext admin hard check", () => {
        afterEach(() => {
            sessionStorage.clear();
        });
        /* Because the session has to be consumed within the session wrapper, I couldn't use renderHook() to
         * get back a returned state value -- the provider itself needs to be accessed from within a component for
         * any provider logic (i.e. adminHardCheck) to be executed. Otherwise, you're just rendering the default
         * Context, which sets everything to undefined, null, or empty. */
        const TestComponent = () => {
            const { activeMembership } = useContext(SessionContext);
            // Conditions to fail
            if (activeMembership?.memberType !== MemberType.PRIME_ADMIN)
                return <>failed</>;
            return <>passed</>;
        };
        test("admin hard check is true when user is admin member type", () => {
            mockUseOktaAuth.mockReturnValue({
                authState: {
                    isAuthenticated: true,
                    accessToken: {
                        claims: { organization: ["DHPrimeAdmins"] },
                    },
                },
            } as any);
            render(
                <SessionProvider key={1} config={{ ...config }}>
                    <TestComponent key={1} />
                </SessionProvider>,
            );
            expect(screen.getByText("passed")).toBeInTheDocument();
        });
        test("admin hard check is false when user is not admin member type", () => {
            mockUseOktaAuth.mockReturnValue({
                authState: {
                    isAuthenticated: true,
                    accessToken: {
                        claims: { organization: ["DHtestOrg-sender"] },
                    },
                },
            } as any);
            render(
                <div>
                    <SessionProvider key={2} config={{ ...config }}>
                        <TestComponent key={2} />
                    </SessionProvider>
                </div>,
            );
            expect(screen.getByText("failed")).toBeInTheDocument();
        });
    });

    describe("idle timer", () => {
        function setup() {
            render(
                <SessionProvider config={{ ...config }}>Test</SessionProvider>,
            );
        }
        beforeEach(() => {
            jest.useFakeTimers();
        });
        afterEach(() => {
            jest.useRealTimers();
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
                jest.setSystemTime(start + timePeriod);
                fireEvent.focus(document);
                expect(mockOnIdle).not.toHaveBeenCalled();
            }
        });

        test("Idle timer triggers at configured time", () => {
            const start = Date.now();
            setup();

            expect(mockOnIdle).not.toHaveBeenCalled();
            jest.setSystemTime(start + config.IDLE_TIMERS.timeout);
            fireEvent.focus(document);
            expect(mockOnIdle).toHaveBeenCalled();
        });
    });
});
