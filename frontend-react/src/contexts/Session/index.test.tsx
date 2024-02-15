import { useOktaAuth } from "@okta/okta-react";
import { cleanup, fireEvent, renderHook, screen } from "@testing-library/react";

import { AppConfig } from "../../config";
import { renderApp } from "../../utils/CustomRenderUtils";
import { MemberType } from "../../utils/OrganizationUtils";

import SessionProvider, { useSessionContext } from ".";


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
    test("admin hard check is true when user is admin member type", () => {
        mockUseOktaAuth.mockReturnValue({authState: {isAuthenticated: true, accessToken: {claims: {organization: ["DHPrimeAdmins"]}}}})
        const { result } = renderHook(() => useSessionContext(), {wrapper: ({children}:any) => <SessionProvider config={config}>{children}</SessionProvider>})
        expect(result.current.activeMembership?.memberType).toBe(MemberType.PRIME_ADMIN)
        cleanup()
    });
    test("admin hard check is false when user is not admin member type", () => {
        mockUseOktaAuth.mockReturnValue({authState: {isAuthenticated: true, accessToken: {claims: {organization: ["DHtestOrg-sender"]}}}})
        const { result } = renderHook(() => useSessionContext(), {wrapper: ({children}:any) => <SessionProvider config={config}>{children}</SessionProvider>})
        expect(result.current.activeMembership?.memberType).not.toBe(MemberType.PRIME_ADMIN)
    });
});

    describe("idle timer", () => {
        function setup() {
            renderApp(<SessionProvider config={config}>Test</SessionProvider>)
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
})
