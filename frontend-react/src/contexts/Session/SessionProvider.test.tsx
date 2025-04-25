import { useOktaAuth } from "@okta/okta-react";
import { fireEvent, render, screen } from "@testing-library/react";

import { useContext } from "react";
import SessionProvider, { SessionContext } from "./SessionProvider";
import { configFixture } from "./useSessionContext.fixtures";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import { isUseragentPreferred } from "../../utils/BrowserUtils";
import { MemberType } from "../../utils/OrganizationUtils";

vi.mock("../../utils/PermissionsUtils");
vi.mock("../../utils/BrowserUtils", () => {
    return {
        isUseragentPreferred: vi.fn(),
    };
});
vi.mock("../../utils/TelemetryService/TelemetryService");

const mockUseOktaAuth = vi.mocked(useOktaAuth);

const mockOnIdle = configFixture.IDLE_TIMERS.onIdle;
const mockUseAppInsightsContext = vi.mocked(useAppInsightsContext);
const mockReactPlugin = mockUseAppInsightsContext();
const mockIsUseragentPreferred = vi.mocked(isUseragentPreferred);

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
            if (activeMembership?.memberType !== MemberType.PRIME_ADMIN) return <>failed</>;
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
                <SessionProvider key={1} config={{ ...configFixture }} rsConsole={{} as any}>
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
                    <SessionProvider key={2} config={{ ...configFixture }} rsConsole={{} as any}>
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
                <SessionProvider config={{ ...configFixture }} rsConsole={{} as any}>
                    Test
                </SessionProvider>,
            );
        }
        beforeEach(() => {
            vi.useFakeTimers();
        });
        afterEach(() => {
            vi.useRealTimers();
        });
        test("Idle timer does not trigger before configured time", () => {
            const testPeriods = [
                configFixture.IDLE_TIMERS.timeout / 3,
                (configFixture.IDLE_TIMERS.timeout / 3) * 2,
                configFixture.IDLE_TIMERS.timeout - 1000 * 60,
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
            vi.setSystemTime(start + configFixture.IDLE_TIMERS.timeout);
            fireEvent.focus(document);
            expect(mockOnIdle).toHaveBeenCalled();
        });
    });

    describe("telemetry custom properties", () => {
        function setup(isUseragentPreferred = true) {
            mockIsUseragentPreferred.mockReturnValue(isUseragentPreferred);
            render(
                <SessionProvider config={{ ...configFixture }} rsConsole={{} as any}>
                    Test
                </SessionProvider>,
            );
        }
        describe("isUserAgentOutdated", () => {
            test("undefined when regex test passes", () => {
                setup();
                expect(mockReactPlugin.customProperties.isUserAgentOutdated).toBe(undefined);
            });

            test("true when test fails", () => {
                setup(false);
                expect(mockReactPlugin.customProperties.isUserAgentOutdated).toBe(true);
            });
        });
    });
});
